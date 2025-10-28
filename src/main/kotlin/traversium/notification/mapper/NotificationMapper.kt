package traversium.notification.mapper

import traversium.notification.db.model.Notification
import traversium.notification.kafka.KafkaStreamData

/**
 * @author Maja Razinger
 */
object NotificationMapper {

    fun KafkaStreamData.toEntity(): Notification {
        return Notification(
            senderId = this.senderId,
            receiverIds = this.receiverIds,
            content = this.message,
            referenceId = this.referenceId
        )
    }

}