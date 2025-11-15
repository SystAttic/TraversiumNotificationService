package traversium.notification.dto

import traversium.notification.mapper.NotificationType
import java.time.OffsetDateTime

/**
 * @author Maja Razinger
 */
data class NotificationDto(
    val senderId: String,
    val recipientId: String,
    val type: NotificationType,
    val collectionReferenceId: Long?,
    val nodeReferenceId: Long?,
    val commentReferenceId: Long?,
    val timestamp: OffsetDateTime,
)