package traversium.notification.mapper

import traversium.notification.db.model.SeenNotificationBundle
import traversium.notification.db.model.UnseenNotification
import traversium.notification.dto.NotificationBundleDto
import traversium.notification.exceptions.NotificationExceptions
import traversium.notification.kafka.NotificationStreamData

/**
 * @author Maja Razinger
 */
object NotificationMapper {

    fun NotificationStreamData.toUnseenEntities(): List<UnseenNotification> {
        return receiverIds.map { receiverId ->
            UnseenNotification(
                senderId = this.senderId,
                receiverId = receiverId,
                collectionReferenceId = this.collectionReferenceId,
                nodeReferenceId = this.nodeReferenceId,
                mediaReferenceId = this.mediaReferenceId,
                commentReferenceId = this.commentReferenceId,
                timestamp = this.timestamp
            )
        }
    }

    fun SeenNotificationBundle.toDto(): NotificationBundleDto {
        return NotificationBundleDto(
            bundleId = this.bundleId ?: throw NotificationExceptions.InvalidNotificationDataException("SeenNotificationBundle bundleId is null"),
            senderIds = this.senderIds,
            type = this.action,
            collectionReferenceId = this.collectionReferenceId,
            nodeReferenceId = this.nodeReferenceId,
            mediaReferenceIds = this.mediaReferenceId,
            commentReferenceId = this.commentReferenceId,
            notificationCount = this.notificationCount,
            firstTimestamp = this.firstTimestamp,
            lastTimestamp = this.lastTimestamp,
            mediaCount = this.mediaCount
        )
    }

}