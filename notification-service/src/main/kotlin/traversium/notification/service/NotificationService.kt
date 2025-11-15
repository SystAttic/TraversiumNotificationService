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
import traversium.notification.db.model.Notification
import traversium.notification.db.repository.NotificationRepository
import traversium.notification.dto.NotificationDto
import traversium.notification.exceptions.NotificationExceptions
import traversium.notification.kafka.NotificationStreamData
import traversium.notification.mapper.NotificationMapper.toDto
import traversium.notification.mapper.NotificationMapper.toEntities
import traversium.notification.mapper.NotificationType
import java.time.OffsetDateTime
import java.util.*

/**
 * @author Maja Razinger
 */
@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val notificationSink: Sinks.Many<NotificationDto>,
    private val firebaseService: FirebaseService
) {
    @Transactional
    fun saveNotification(streamData: NotificationStreamData): List<Notification> {
        val notificationType = findNotificationType(streamData)
        val entities = streamData.toEntities()
            .map { it.copy(action = notificationType) }

        val savedNotifications = notificationRepository.saveAll(entities)

        savedNotifications.forEach {
            val notificationDto = NotificationDto(
                senderId = it.senderId ?: throw NotificationExceptions.InvalidNotificationDataException("senderId is null"),
                recipientId = it.receiverId ?: throw NotificationExceptions.InvalidNotificationDataException("receiverId is null"),
                collectionReferenceId = it.collectionReferenceId,
                nodeReferenceId = it.nodeReferenceId,
                commentReferenceId = it.commentReferenceId,
                type = notificationType,
                timestamp = it.timestamp!!
            )
            notificationSink.tryEmitNext(notificationDto)
        }

        return savedNotifications
    }

    fun getUnseenNotificationsCount(): Long =
        notificationRepository.countByReceiverIdAndSeenFalse(getFirebaseIdFromContext())

    fun getNotificationsForUser(offset: Int, limit: Int): List<NotificationDto> {
        val pageable = PageRequest.of(offset / limit, limit)
        return notificationRepository.findByReceiverId(getFirebaseIdFromContext(), pageable)
            .map { it.toDto() }.toList()
    }

    fun sendNotificationsFlux(): Flux<ServerSentEvent<NotificationDto>>? {
        val userId = getFirebaseIdFromContext()
        return notificationSink.asFlux()
            .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, BufferOverflowStrategy.DROP_OLDEST)
            .publishOn(Schedulers.boundedElastic())
            .filter { it.recipientId == userId }
            .map { notificationDto ->
                ServerSentEvent.builder(notificationDto)
                    .id(UUID.randomUUID().toString())
                    .event(if (notificationDto.type == NotificationType.HEALTHCHECK) "heartbeat" else "Notification")
                    .data(notificationDto)
                    .build()
            }
    }

    @Scheduled(fixedRate = 30_000)
    fun sendHealthCheck() {
        val heartbeat = NotificationDto(
            senderId = "system",
            recipientId = "ALL",
            collectionReferenceId = null,
            nodeReferenceId = null,
            commentReferenceId = null,
            type = NotificationType.HEALTHCHECK,
            timestamp = OffsetDateTime.now()
        )

        notificationSink.tryEmitNext(heartbeat)
    }

    private fun findNotificationType(notificationStreamData: NotificationStreamData): NotificationType {
        val collectionId = notificationStreamData.collectionReferenceId
        val nodeId = notificationStreamData.nodeReferenceId
        val commentId = notificationStreamData.commentReferenceId
        val action = notificationStreamData.action

        return when {
            collectionId != null && nodeId != null -> when (action) {
                "CREATE" -> NotificationType.CREATE_NODE
                "UPDATE" -> NotificationType.UPDATE_NODE
                "DELETE" -> NotificationType.DELETE_NODE
                else -> throw IllegalArgumentException("Unknown action: $action")
            }
            nodeId != null && commentId != null -> when (action) {
                "ADD" -> NotificationType.ADD_COMMENT
                "REPLY" -> NotificationType.REPLY_COMMENT
                "REMOVE" -> NotificationType.REMOVE_COMMENT
                else -> throw IllegalArgumentException("Unknown action: $action")
            }
            collectionId != null -> when (action) {
                "CREATE" -> NotificationType.CREATE_COLLECTION
                "UPDATE" -> NotificationType.UPDATE_COLLECTION
                "DELETE" -> NotificationType.DELETE_COLLECTION
                "ADD_COLLABORATOR" -> NotificationType.ADD_COLLABORATOR
                "REMOVE_COLLABORATOR" -> NotificationType.REMOVE_COLLABORATOR
                else -> throw IllegalArgumentException("Unknown action: $action")
            }
            else -> throw IllegalArgumentException("Invalid notification data")
        }
    }

    private fun getFirebaseIdFromContext() =
        firebaseService.extractUidFromToken(SecurityContextHolder.getContext().authentication.credentials as String)
}