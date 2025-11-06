package traversium.notification.exceptions

/**
 * @author Maja Razinger
 */
class NotificationExceptions {

    class InvalidNotificationDataException(message: String) : RuntimeException("Invalid notification data: $message")
}