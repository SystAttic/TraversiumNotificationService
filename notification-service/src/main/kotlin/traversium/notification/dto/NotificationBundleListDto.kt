package traversium.notification.dto

/**
 * @author Maja Razinger
 */
data class NotificationBundleListDto(
    val unseenBundles: List<NotificationBundleDto>,
    val seenBundles: List<NotificationBundleDto>
)
