package traversium.notification.kafka

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.errors.RecordDeserializationException
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.*
import org.springframework.kafka.support.TopicPartitionOffset
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.util.backoff.FixedBackOff
import traversium.notification.service.NotificationService
import kotlin.reflect.KClass

/**
 * @author Maja Razinger
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.kafka", name = ["bootstrap-servers"])
class KafkaConfiguration(val kafkaProperties: KafkaProperties) {

    @Bean
    fun notificationKafkaConsumer(notificationService: NotificationService) = KafkaConsumer(notificationService)

    @Bean
    fun consumerFactory(
        @Qualifier("kafkaObjectMapper") kafkaObjectMapper: ObjectMapper
    ): ConsumerFactory<String, NotificationStreamData> {
        return DefaultKafkaConsumerFactory(
            kafkaConsumerConfig(
                kafkaProperties.bootstrapServers,
                "${kafkaProperties.consumerGroupPrefix?.let { "$it-" } ?: ""}notification-group",
                kafkaProperties.fetchMaxBytes,
                kafkaProperties.maxPartitionFetchBytes,
                NotificationStreamData::class
            ),
            StringDeserializer(),
            JsonDeserializer(NotificationStreamData::class.java, kafkaObjectMapper)
        )
    }

    @Bean
    fun notificationEventListenerContainer(
        notificationKafkaConsumer: KafkaConsumer,
        consumerFactory: ConsumerFactory<String, NotificationStreamData>
    ): KafkaMessageListenerContainer<String, NotificationStreamData> = KafkaMessageListenerContainer(
        consumerFactory,
        kafkaContainerProperties(
            kafkaProperties.topic,
            kafkaProperties.partitions,
            notificationKafkaConsumer
        )
    ).apply {
        commonErrorHandler = recordDeserializationErrorHandler()
    }

    @Bean
    fun kafkaObjectMapper(): ObjectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private fun kafkaConsumerConfig(
        bootstrapServers: String,
        groupId: String,
        fetchMaxBytes: Int,
        maxPartitionFetchBytes: Int,
        clazz: KClass<*>,
    ): HashMap<String, Any> =
        HashMap<String, Any>().apply {
            this[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
            this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
            this[ConsumerConfig.ISOLATION_LEVEL_CONFIG] = "read_committed"
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
            this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = ErrorHandlingDeserializer::class.java
            this[ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS] = StringDeserializer::class.java
            this[ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS] = JsonDeserializer::class.java
            this[ConsumerConfig.FETCH_MAX_BYTES_CONFIG] = fetchMaxBytes
            this[ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG] = maxPartitionFetchBytes
            this[JsonDeserializer.VALUE_DEFAULT_TYPE] = clazz.java
            this[ConsumerConfig.GROUP_ID_CONFIG] = groupId
        }

    private fun recordDeserializationErrorHandler() = object : DefaultErrorHandler(FixedBackOff(0, 0)) {
        override fun handleOtherException(
            thrownException: Exception,
            consumer: Consumer<*, *>,
            container: MessageListenerContainer,
            batchListener: Boolean,
        ) {
            when (thrownException) {
                is RecordDeserializationException -> {
                    logger().error("Kafka consumer (listenerId: ${container.listenerId}, groupId: ${consumer.groupMetadata()}) deserialization failed with the following exception: ${thrownException.cause?.cause}")
                    // The following line ensures that the problematic record is skipped
                    consumer.seek(thrownException.topicPartition(), thrownException.offset() + 1)
                }

                else -> {
                    super.handleOtherException(thrownException, consumer, container, batchListener)
                }
            }
        }
    }

    private fun <T> kafkaContainerProperties(
        topic: String,
        partitions: Set<Int>,
        listener: MessageListener<String, T>
    ): ContainerProperties {
        val topics = partitions.map { TopicPartitionOffset(topic, it) }.toTypedArray()
        return (if (topics.isNotEmpty()) ContainerProperties(*topics) else ContainerProperties(topic)).apply {
            isSyncCommits = true
            ackMode = ContainerProperties.AckMode.RECORD
            messageListener = listener
        }
    }
}