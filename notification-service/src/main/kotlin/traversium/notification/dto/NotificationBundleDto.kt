package traversium.notification.dto

import traversium.notification.mapper.NotificationType
import java.time.OffsetDateTime

/**
 * @author Maja Razinger
 */
data class NotificationBundleDto(
    val bundleId: String,
    val senderIds: List<String>,
    val type: NotificationType,
    val collectionReferenceId: Long?,
    val nodeReferenceId: Long?,
    val mediaReferenceIds: Long?,
    val mediaCount: Int?,
    val commentReferenceId: Long?,
    val notificationCount: Int,
    val firstTimestamp: OffsetDateTime,
    val lastTimestamp: OffsetDateTime,
)
