package traversium.notification.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.kafka.listener.MessageListener
import traversium.notification.service.NotificationService

/**
 * @author Maja Razinger
 */
class KafkaConsumer(
    private val notificationService: NotificationService,
): MessageListener<String, KafkaStreamData>, Logging {

    override fun onMessage(data: ConsumerRecord<String?, KafkaStreamData?>) {
        val payload = data.value() ?: run {
            logger.warn { "Received null KafkaStreamData payload, skipping processing." }
            return
        }
        notificationService.saveNotification(payload)
    }
}

// TODO: SSE for recievers to get notifications in real-time