package traversium.notification.mapper

import traversium.notification.db.model.Notification
import traversium.notification.kafka.NotificationStreamData

/**
 * @author Maja Razinger
 */
object NotificationMapper {

    fun NotificationStreamData.toEntity(): Notification {
        return Notification(
            senderId = this.senderId,
            receiverIds = this.receiverIds,
            collectionReferenceId = this.collectionReferenceId,
            nodeReferenceId = this.nodeReferenceId,
            commentReferenceId = this.commentReferenceId,
            timestamp = this.timestamp,
        )
    }

}