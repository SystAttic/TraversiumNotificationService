package traversium.notification.kafka

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

/**
 * @author Maja Razinger
 */
@ConfigurationProperties(prefix = "spring.kafka")
class KafkaProperties @ConstructorBinding constructor(
    val bootstrapServers: String = "localhost:9092",
    val topic: String = "notification-topic",
    val partitions: Set<Int> = emptySet(),
    val consumerGroupPrefix: String?,
    val maxPartitionFetchBytes: Int = 1048576,
    val fetchMaxBytes: Int = 1048576,
)