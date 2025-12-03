package traversium.notification.service

import org.springframework.data.domain.PageRequest
import org.springframework.http.codec.ServerSentEvent
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.BufferOverflowStrategy
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import reactor.util.concurrent.Queues
import traversium.notification.db.model.SeenNotificationBundle
import traversium.notification.db.model.UnseenNotification
import traversium.notification.db.repository.SeenNotificationBundleRepository
import traversium.notification.db.repository.UnseenNotificationRepository
import traversium.notification.dto.BundleIdDto
import traversium.notification.dto.NotificationBundleDto
import traversium.notification.dto.NotificationBundleListDto
import traversium.notification.kafka.ActionType
import traversium.notification.kafka.NotificationStreamData
import traversium.notification.mapper.NotificationMapper.toDto
import traversium.notification.mapper.NotificationMapper.toUnseenEntities
import traversium.notification.mapper.NotificationType
import traversium.notification.util.BundleIdGenerator
import java.util.*

/**
 * @author Maja Razinger
 */
@Service
class NotificationService(
    private val unseenNotificationRepository: UnseenNotificationRepository,
    private val seenNotificationBundleRepository: SeenNotificationBundleRepository,
    private val bundleIdSink: Sinks.Many<BundleIdDto>,
    private val firebaseService: FirebaseService
) {
    @Transactional
    fun saveNotification(streamData: NotificationStreamData): List<UnseenNotification> {
        val notificationType = findNotificationType(streamData)
        val entities = streamData.toUnseenEntities()
            .map { it.copy(action = notificationType) }

        val savedNotifications = unseenNotificationRepository.saveAll(entities)

        return savedNotifications
    }

    fun getUnseenNotificationsCount(): Long {
        val userId = getFirebaseIdFromContext()
        val unseenNotifications = unseenNotificationRepository.findAll()
            .filter { it.receiverId == userId }

        if (unseenNotifications.isEmpty()) {
            return 0
        }

        val bundleCount = unseenNotifications.groupBy { notification ->
            BundleIdGenerator.generateBundleId(notification)
        }.size

        return bundleCount.toLong()
    }

    @Transactional
    fun getNotificationsForUser(offset: Int, limit: Int): NotificationBundleListDto {
        val userId = getFirebaseIdFromContext()

        val pageable = PageRequest.of(offset / limit, limit)
        val unseenNotifications = unseenNotificationRepository.findByReceiverId(userId, pageable).content

        val unseenBundlesData = if (unseenNotifications.isNotEmpty()) {
            unseenNotifications.groupBy { notification ->
                BundleIdGenerator.generateBundleId(notification)
            }
        } else {
            emptyMap()
        }

        val unseenBundlesList = unseenBundlesData.map { (bundleId, notifications) ->
            convertUnseenGroupToDto(bundleId, notifications)
        }.sortedByDescending { it.lastTimestamp }

        val seenBundles = if (unseenBundlesList.size >= limit) {
            emptyList()
        } else {
            val remainingLimit = limit - unseenBundlesList.size
            val seenPageable = PageRequest.of(0, remainingLimit)
            seenNotificationBundleRepository.findByReceiverId(userId, seenPageable)
                .map { it.toDto() }.toList()
        }

        if (unseenNotifications.isNotEmpty()) {
            createBundlesFromUnseen(unseenNotifications)
        }

        return NotificationBundleListDto(
            unseenBundles = unseenBundlesList,
            seenBundles = seenBundles
        )
    }

    @Transactional
    fun getUnseenNotificationsForUser(offset: Int, limit: Int): List<NotificationBundleDto> {
        val userId = getFirebaseIdFromContext()

        val pageable = PageRequest.of(offset / limit, limit)
        val unseenNotifications = unseenNotificationRepository.findByReceiverId(userId, pageable).content

        if (unseenNotifications.isEmpty()) {
            return emptyList()
        }

        val unseenBundlesData = unseenNotifications.groupBy { notification ->
            BundleIdGenerator.generateBundleId(notification)
        }

        val unseenBundlesList = unseenBundlesData.map { (bundleId, notifications) ->
            convertUnseenGroupToDto(bundleId, notifications)
        }.sortedByDescending { it.lastTimestamp }

        createBundlesFromUnseen(unseenNotifications)

        return unseenBundlesList
    }

    fun getSeenNotificationsForUser(offset: Int, limit: Int): List<NotificationBundleDto> {
        val userId = getFirebaseIdFromContext()

        val pageable = PageRequest.of(offset / limit, limit)
        return seenNotificationBundleRepository.findByReceiverId(userId, pageable)
            .map { it.toDto() }
            .toList()
    }

    @Transactional
    fun createBundlesFromUnseen(unseenNotifications: List<UnseenNotification>): Map<String, List<UnseenNotification>> {
        val bundleGroups = unseenNotifications.groupBy { notification ->
            BundleIdGenerator.generateBundleId(notification)
        }

        bundleGroups.forEach { (bundleId, notifications) ->
            val firstNotification = notifications.first()
            val bundle = SeenNotificationBundle(
                bundleId = bundleId,
                receiverId = firstNotification.receiverId!!,
                senderIds = notifications.map { it.senderId!! }.distinct(),
                action = firstNotification.action!!,
                collectionReferenceId = firstNotification.collectionReferenceId,
                nodeReferenceId = firstNotification.nodeReferenceId,
                mediaReferenceIds = notifications.mapNotNull { it.mediaReferenceId }.distinct(),
                commentReferenceId = firstNotification.commentReferenceId,
                notificationCount = notifications.size,
                firstTimestamp = notifications.minOf { it.timestamp!! },
                lastTimestamp = notifications.maxOf { it.timestamp!! }
            )
            seenNotificationBundleRepository.save(bundle)
        }

        unseenNotificationRepository.deleteAll(unseenNotifications)
        return bundleGroups
    }

    private fun convertUnseenGroupToDto(bundleId: String, notifications: List<UnseenNotification>): NotificationBundleDto {
        val firstNotification = notifications.first()
        return NotificationBundleDto(
            bundleId = bundleId,
            senderIds = notifications.map { it.senderId!! }.distinct(),
            type = firstNotification.action!!,
            collectionReferenceId = firstNotification.collectionReferenceId,
            nodeReferenceId = firstNotification.nodeReferenceId,
            mediaReferenceIds = notifications.mapNotNull { it.mediaReferenceId }.distinct(),
            commentReferenceId = firstNotification.commentReferenceId,
            notificationCount = notifications.size,
            firstTimestamp = notifications.minOf { it.timestamp!! },
            lastTimestamp = notifications.maxOf { it.timestamp!! }
        )
    }

    fun sendBundleIdsFlux(): Flux<ServerSentEvent<BundleIdDto>>? {
        val userId = getFirebaseIdFromContext()
        return bundleIdSink.asFlux()
            .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, BufferOverflowStrategy.DROP_OLDEST)
            .publishOn(Schedulers.boundedElastic())
            .filter { bundleIdDto ->
                bundleIdDto.bundleId == "HEALTHCHECK" || bundleIdDto.bundleId.startsWith("$userId-")
            }
            .map { bundleIdDto ->
                ServerSentEvent.builder(bundleIdDto)
                    .id(UUID.randomUUID().toString())
                    .event("NotificationBundle")
                    .data(bundleIdDto)
                    .build()
            }
    }

    @Scheduled(fixedRate = 30_000)
    fun sendHealthCheck() {
        val heartbeat = BundleIdDto(
            bundleId = "HEALTHCHECK"
        )

        bundleIdSink.tryEmitNext(heartbeat)
    }

    private fun findNotificationType(notificationStreamData: NotificationStreamData): NotificationType {
        val collectionId = notificationStreamData.collectionReferenceId
        val nodeId = notificationStreamData.nodeReferenceId
        val mediaId = notificationStreamData.mediaReferenceId
        val commentId = notificationStreamData.commentReferenceId
        val action = notificationStreamData.action

        return when {
            collectionId != null && nodeId != null && mediaId != null && commentId == null -> when (action) {
                ActionType.ADD -> NotificationType.ADD_PHOTO
                ActionType.REMOVE -> NotificationType.REMOVE_PHOTO
                else -> throw IllegalArgumentException("Unknown action: $action")
            }
            collectionId != null && nodeId != null -> when (action) {
                ActionType.CREATE -> NotificationType.CREATE_MOMENT
                ActionType.DELETE -> NotificationType.DELETE_MOMENT
                ActionType.REARRANGE -> NotificationType.REARRANGE_MOMENTS
                ActionType.CHANGE_TITLE -> NotificationType.CHANGE_MOMENT_TITLE
                ActionType.CHANGE_COVER_PHOTO -> NotificationType.CHANGE_MOMENT_COVER_PHOTO
                ActionType.CHANGE_DESCRIPTION -> NotificationType.CHANGE_MOMENT_DESCRIPTION
                else -> throw IllegalArgumentException("Unknown action: $action")
            }
            mediaId != null && commentId != null -> when (action) {
                ActionType.ADD -> NotificationType.ADD_COMMENT
                ActionType.REPLY -> NotificationType.REPLY_COMMENT
                else -> throw IllegalArgumentException("Unknown action: $action")
            }
            mediaId != null -> when (action) {
                ActionType.LIKE -> NotificationType.LIKE_PHOTO
                else -> throw IllegalArgumentException("Unknown action: $action")
            }
            collectionId != null -> when (action) {
                ActionType.CREATE -> NotificationType.CREATE_TRIP
                ActionType.DELETE -> NotificationType.DELETE_TRIP
                ActionType.ADD_COLLABORATOR -> NotificationType.ADD_COLLABORATOR
                ActionType.REMOVE_COLLABORATOR -> NotificationType.REMOVE_COLLABORATOR
                ActionType.ADD_VIEWER -> NotificationType.ADD_VIEWER
                ActionType.REMOVE_VIEWER -> NotificationType.REMOVE_VIEWER
                ActionType.CHANGE_TITLE -> NotificationType.CHANGE_TRIP_TITLE
                ActionType.CHANGE_COVER_PHOTO -> NotificationType.CHANGE_TRIP_COVER_PHOTO
                ActionType.CHANGE_DESCRIPTION -> NotificationType.CHANGE_TRIP_DESCRIPTION
                else -> throw IllegalArgumentException("Unknown action: $action")
            }
            else -> when (action) {
                ActionType.FOLLOW -> NotificationType.FOLLOW_USER
                else -> throw IllegalArgumentException("Unknown action: $action")
            }
        }
    }

    private fun getFirebaseIdFromContext() =
        firebaseService.extractUidFromToken(SecurityContextHolder.getContext().authentication.credentials as String)
}
