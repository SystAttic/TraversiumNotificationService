package traversium.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import traversium.notification.db.repository.NotificationRepository
import traversium.notification.kafka.NotificationStreamData
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import kotlin.test.Test

/**
 * @author Maja Razinger
 */
@SpringBootTest
@ExtendWith(SpringExtension::class)
@EmbeddedKafka(partitions = 1, topics = ["test-notification-topic"], bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@ActiveProfiles("test")
class KafkaConsumerTests(
) {

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun testKafkaConsumer() {
        val testData = NotificationStreamData(
            timestamp = OffsetDateTime.now(),
            senderId = "sender1",
            receiverIds = listOf("receiver1", "receiver2"),
            collectionReferenceId = 123L,
            nodeReferenceId = null,
            commentReferenceId = null,
            action = "CREATE"
        )

        val json = ObjectMapper().registerModule(JavaTimeModule()).writeValueAsString(testData)

        kafkaTemplate.send("test-notification-topic", "key1", json)

        await().atMost(5, TimeUnit.SECONDS).until {
            notificationRepository.count() == 1L
        }

        val savedNotification = notificationRepository.findAll().first()
        assertThat(savedNotification.senderId).isEqualTo("sender1")
        assertThat(savedNotification.receiverIds).containsExactly("receiver1", "receiver2")
        assertThat(savedNotification.collectionReferenceId).isEqualTo(123L)

        notificationRepository.delete(savedNotification)
    }
}