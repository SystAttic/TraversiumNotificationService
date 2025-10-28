package traversium.notification.mapper

import traversium.notification.db.model.Notification
import traversium.notification.dto.NotificationDto
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
            senderId = this.senderId ?: throw NullPointerException("senderId"),
            recipientId = this.receiverId ?: throw NullPointerException("receiverId"),
            collectionReferenceId = this.collectionReferenceId,
            nodeReferenceId = this.nodeReferenceId,
            commentReferenceId = this.commentReferenceId,
            type = this.action ?: throw NullPointerException("action")
        )
    }

}