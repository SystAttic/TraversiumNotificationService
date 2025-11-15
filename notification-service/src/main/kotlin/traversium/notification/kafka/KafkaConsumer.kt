package traversium.notification.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.kafka.listener.MessageListener
import traversium.notification.exceptions.NotificationExceptions
import traversium.notification.service.NotificationService

/**
 * @author Maja Razinger
 */
class KafkaConsumer(
    private val notificationService: NotificationService,
) : MessageListener<String, NotificationStreamData>, Logging {

    override fun onMessage(data: ConsumerRecord<String?, NotificationStreamData?>) {
        val payload = data.value() ?: run {
            logger.warn { "Received null KafkaStreamData payload on topic=${data.topic()}, partition=${data.partition()}, offset=${data.offset()}" }
            return
        }

        try {
            logger.debug { "Processing notification: topic=${data.topic()}, partition=${data.partition()}, offset=${data.offset()}, payload=$payload" }
            val savedNotifications = notificationService.saveNotification(payload)
            logger.info { "Successfully processed notification: saved ${savedNotifications.size} notification(s), offset=${data.offset()}" }
        } catch (e: NotificationExceptions.InvalidNotificationDataException) {
            logger.error(e) {
                "Invalid notification data, skipping message: topic=${data.topic()}, partition=${data.partition()}, offset=${data.offset()}, payload=$payload"
            }
        } catch (e: Exception) {
            logger.error(e) {
                "Error processing notification message: topic=${data.topic()}, partition=${data.partition()}, offset=${data.offset()}, payload=$payload"
            }
            throw e
        }
    }
}