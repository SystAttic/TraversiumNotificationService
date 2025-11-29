package traversium.notification.util

import traversium.notification.db.model.UnseenNotification
import traversium.notification.exceptions.NotificationExceptions
import traversium.notification.mapper.NotificationType

/**
 * @author Maja Razinger
 */
object BundleIdGenerator {

    fun generateBundleId(notification: UnseenNotification): String {
        val receiverId = notification.receiverId ?: throw NotificationExceptions.InvalidNotificationDataException("receiverId cannot be null")
        val senderId = notification.senderId ?: throw NotificationExceptions.InvalidNotificationDataException("senderId cannot be null")
        val type = notification.action ?: throw NotificationExceptions.InvalidNotificationDataException("action cannot be null")

        return when (type) {
            NotificationType.LIKE_PHOTO ->
                buildBundleId(receiverId, type, notification.mediaReferenceId)

            NotificationType.ADD_PHOTO, NotificationType.REMOVE_PHOTO ->
                buildBundleIdWithSender(receiverId, senderId, type, notification.collectionReferenceId, notification.nodeReferenceId)

            NotificationType.CREATE_MOMENT, NotificationType.DELETE_MOMENT,
            NotificationType.REARRANGE_MOMENTS->
                buildBundleIdWithSender(receiverId, senderId, type, notification.collectionReferenceId)

            NotificationType.CHANGE_MOMENT_TITLE,
            NotificationType.CHANGE_MOMENT_COVER_PHOTO, NotificationType.CHANGE_MOMENT_DESCRIPTION ->
                buildBundleId(receiverId, type, notification.collectionReferenceId, notification.nodeReferenceId)

            NotificationType.ADD_COMMENT, NotificationType.REPLY_COMMENT ->
                buildBundleIdWithSender(receiverId, senderId, type, notification.mediaReferenceId, notification.commentReferenceId)

            NotificationType.CREATE_TRIP, NotificationType.DELETE_TRIP,
            NotificationType.CHANGE_TRIP_TITLE, NotificationType.CHANGE_TRIP_DESCRIPTION,
            NotificationType.CHANGE_TRIP_COVER_PHOTO,
            NotificationType.ADD_COLLABORATOR, NotificationType.ADD_VIEWER,
            NotificationType.REMOVE_COLLABORATOR, NotificationType.REMOVE_VIEWER ->
                buildBundleIdWithSender(receiverId, senderId, type, notification.collectionReferenceId)

            NotificationType.FOLLOW_USER ->
                buildBundleId(receiverId, type)

            NotificationType.HEALTHCHECK ->
                throw IllegalArgumentException("HEALTHCHECK notifications should not be bundled")
            else ->
                throw IllegalArgumentException("Unknown notification type: $type")
        }
    }

    private fun buildBundleId(receiverId: String, type: NotificationType,  vararg referenceIds: Long?): String {
        val parts = mutableListOf(receiverId)
        referenceIds.forEach { id ->
            if (id != null) {
                parts.add(id.toString())
            }
        }
        parts.add(type.name)
        return parts.joinToString("-")
    }

    private fun buildBundleIdWithSender(receiverId: String, senderId: String, type: NotificationType, vararg referenceIds: Long?): String {
        val parts = mutableListOf(receiverId, senderId)
        referenceIds.forEach { id ->
            if (id != null) {
                parts.add(id.toString())
            }
        }
        parts.add(type.name)
        return parts.joinToString("-")
    }
}
