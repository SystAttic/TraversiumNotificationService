package traversium.notification.service

import jakarta.persistence.EntityManager
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Sinks
import traversium.commonmultitenancy.TenantContext
import traversium.commonmultitenancy.TenantUtils
import traversium.notification.db.repository.UnseenNotificationRepository
import traversium.notification.dto.BundleIdDto
import traversium.notification.util.BundleIdGenerator

/**
 * @author Maja Razinger
 */
@Service
class NotificationBundlingService(
    private val unseenNotificationRepository: UnseenNotificationRepository,
    private val bundleIdSink: Sinks.Many<BundleIdDto>,
    private val entityManager: EntityManager
) : Logging {
    private val emittedBundleIds = mutableSetOf<String>()

    @Scheduled(fixedDelayString = "\${notification.bundling.interval:5000}")
    @Transactional(readOnly = true)
    fun processUnseenNotifications() {
        try {
            val schemaNames = getAllTenantSchemas()

            schemaNames.forEach { schemaName ->
                try {
                    setSchemaForTenant(schemaName)
                    generateBundles()
                } catch (e: Exception) {
                    logger.error("Error processing unseen notifications for schema $schemaName", e)
                } finally {
                    TenantContext.clear()
                }
            }

        } catch (e: Exception) {
            logger.error("Error processing unseen notifications", e)
        }
    }

    private fun setSchemaForTenant(schemaName: String) {
        var rawTenantId = schemaName
        while (rawTenantId.startsWith("tenant_")) {
            rawTenantId = rawTenantId.removePrefix("tenant_")
        }

        TenantContext.setTenant(TenantUtils.sanitizeTenantIdForSchema(rawTenantId))
    }

    private fun generateBundles() {
        val unseenNotifications = unseenNotificationRepository.findAll()

        if (unseenNotifications.isEmpty()) {
            return
        }

        val bundleGroups = unseenNotifications.groupBy { notification ->
            BundleIdGenerator.generateBundleId(notification)
        }

        bundleGroups.keys.forEach { bundleId ->
            if (!emittedBundleIds.contains(bundleId)) {
                val bundleIdDto = BundleIdDto(bundleId)
                bundleIdSink.tryEmitNext(bundleIdDto)
                emittedBundleIds.add(bundleId)
                logger.debug("Emitted bundle ID: $bundleId")
            }
        }
    }

    private fun getAllTenantSchemas(): List<String> {
        return try {
            val query = entityManager.createNativeQuery(
                """
                SELECT schema_name
                FROM information_schema.schemata
                WHERE schema_name NOT IN ('pg_catalog', 'information_schema')
                AND schema_name NOT LIKE 'pg_%'
                ORDER BY schema_name
                """
            )
            @Suppress("UNCHECKED_CAST")
            query.resultList as List<String>
        } catch (e: Exception) {
            logger.error(e) { "Error fetching tenant schemas: ${e.message}" }
            emptyList()
        }
    }
}
