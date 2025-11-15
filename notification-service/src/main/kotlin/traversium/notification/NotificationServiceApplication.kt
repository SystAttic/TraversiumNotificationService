package traversium.notification

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import traversium.notification.kafka.KafkaProperties

@SpringBootApplication
@EnableConfigurationProperties(KafkaProperties::class)
@EnableScheduling
class NotificationServiceApplication

fun main(args: Array<String>) {
    runApplication<NotificationServiceApplication>(*args)
}
