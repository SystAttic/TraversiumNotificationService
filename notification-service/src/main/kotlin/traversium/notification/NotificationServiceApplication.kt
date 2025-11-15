package traversium.notification

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling
import traversium.commonmultitenancy.FlywayTenantMigration
import traversium.commonmultitenancy.MultiTenantAutoConfiguration
import traversium.notification.kafka.KafkaProperties

@SpringBootApplication
@EnableConfigurationProperties(KafkaProperties::class)
@EnableScheduling
@Import(MultiTenantAutoConfiguration::class, FlywayTenantMigration::class)
class NotificationServiceApplication

fun main(args: Array<String>) {
    runApplication<NotificationServiceApplication>(*args)
}
