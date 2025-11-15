package traversium.notification.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Headers
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.kafka.listener.MessageListener
import traversium.commonmultitenancy.TenantContext
import traversium.commonmultitenancy.TenantUtils
import traversium.notification.exceptions.NotificationExceptions
import traversium.notification.service.NotificationService
import java.nio.charset.StandardCharsets

/**
 * @author Maja Razinger
 */
class KafkaConsumer(
    private val notificationService: NotificationService,
) : MessageListener<String, NotificationStreamData>, Logging {

    companion object {
        private const val TENANT_HEADER_KEY = "tenantId"
        private const val DEFAULT_TENANT = "public"
    }

    override fun onMessage(data: ConsumerRecord<String?, NotificationStreamData?>) {
        val payload = data.value() ?: run {
            logger.warn { "Received null KafkaStreamData payload on topic=${data.topic()}, partition=${data.partition()}, offset=${data.offset()}" }
            return
        }

        val tenantId = extractTenantFromHeaders(data.headers())
        logger.debug { "Extracted tenant ID: $tenantId from Kafka headers" }

        try {
            TenantContext.setTenant(TenantUtils.sanitizeTenantIdForSchema(tenantId))

            logger.debug { "Processing notification for tenant=$tenantId: topic=${data.topic()}, partition=${data.partition()}, offset=${data.offset()}, payload=$payload" }
            val savedNotifications = notificationService.saveNotification(payload)
            logger.info { "Successfully processed notification for tenant=$tenantId: saved ${savedNotifications.size} notification(s), offset=${data.offset()}" }
        } catch (e: NotificationExceptions.InvalidNotificationDataException) {
            logger.error(e) {
                "Invalid notification data, skipping message: tenant=$tenantId, topic=${data.topic()}, partition=${data.partition()}, offset=${data.offset()}, payload=$payload"
            }
        } catch (e: Exception) {
            logger.error(e) {
                "Error processing notification message: tenant=$tenantId, topic=${data.topic()}, partition=${data.partition()}, offset=${data.offset()}, payload=$payload"
            }
            throw e
        } finally {
            TenantContext.clear()
        }
    }

    private fun extractTenantFromHeaders(headers: Headers): String {
        val tenantHeader = headers.lastHeader(TENANT_HEADER_KEY)
        return if (tenantHeader != null) {
            String(tenantHeader.value(), StandardCharsets.UTF_8)
        } else {
            DEFAULT_TENANT
        }
    }
}