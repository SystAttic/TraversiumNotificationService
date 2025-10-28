package traversium.notification.dto

import traversium.notification.mapper.NotificationType

/**
 * @author Maja Razinger
 */
data class NotificationDto(
    val senderId: String,
    val recipientIds: List<String>,
    val type: NotificationType,
    val collectionReferenceId: Long?,
    val nodeReferenceId: Long?,
    val commentReferenceId: Long?,
)