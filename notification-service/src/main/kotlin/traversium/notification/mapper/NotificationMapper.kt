package traversium.notification.mapper

import traversium.notification.db.model.Notification
import traversium.notification.dto.NotificationDto
import traversium.notification.exceptions.NotificationExceptions
import traversium.notification.kafka.NotificationStreamData

/**
 * @author Maja Razinger
 */
object NotificationMapper {

    fun NotificationStreamData.toEntities(): List<Notification> {
        return receiverIds.map { receiverId ->
            Notification(
                senderId = this.senderId,
                receiverId = receiverId,
                collectionReferenceId = this.collectionReferenceId,
                nodeReferenceId = this.nodeReferenceId,
                commentReferenceId = this.commentReferenceId,
                timestamp = this.timestamp
            )
        }
    }

    fun Notification.toDto(): NotificationDto {
        return NotificationDto(
            senderId = this.senderId ?: throw NotificationExceptions.InvalidNotificationDataException("senderId is not set"),
            recipientId = this.receiverId ?: throw NotificationExceptions.InvalidNotificationDataException("receiverId is not set"),
            collectionReferenceId = this.collectionReferenceId,
            nodeReferenceId = this.nodeReferenceId,
            commentReferenceId = this.commentReferenceId,
            type = this.action ?: throw NotificationExceptions.InvalidNotificationDataException("action is not set"),
            timestamp = this.timestamp ?: throw NotificationExceptions.InvalidNotificationDataException("timestamp is not set"),
        )
    }

}