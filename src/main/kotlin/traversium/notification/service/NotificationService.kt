package traversium.notification.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Sinks
import traversium.notification.db.model.Notification
import traversium.notification.db.repository.NotificationRepository
import traversium.notification.dto.NotificationDto
import traversium.notification.kafka.NotificationStreamData
import traversium.notification.mapper.NotificationMapper.toDto
import traversium.notification.mapper.NotificationMapper.toEntities
import traversium.notification.mapper.NotificationType

/**
 * @author Maja Razinger
 */
@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val notificationSink: Sinks.Many<NotificationDto>
) {
    @Transactional
    fun saveNotification(streamData: NotificationStreamData): List<Notification> {
        val notificationType = findNotificationType(streamData)
        val entities = streamData.toEntities()
            .map { it.copy(action = notificationType) }

        val savedNotifications = notificationRepository.saveAll(entities)

        savedNotifications.forEach {
            val notificationDto = NotificationDto(
                senderId = it.senderId ?: throw NullPointerException("senderId"),
                recipientId = it.receiverId ?: throw NullPointerException("receiverId"),
                collectionReferenceId = it.collectionReferenceId,
                nodeReferenceId = it.nodeReferenceId,
                commentReferenceId = it.commentReferenceId,
                type = notificationType
            )
            notificationSink.tryEmitNext(notificationDto)
        }

        return savedNotifications
    }

    fun getUnseenNotificationsCount(userId: String): Long =
        notificationRepository.countByReceiverIdAndSeenFalse(userId)

    fun getNotificationsForUser(userId: String, offset: Int, limit: Int): List<NotificationDto> {
        val pageable = PageRequest.of(offset / limit, limit)
        return notificationRepository.findByReceiverId(userId, pageable)
            .map { it.toDto() }.toList()
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

}