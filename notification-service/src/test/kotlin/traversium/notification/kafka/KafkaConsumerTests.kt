package traversium.notification.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.assertj.core.api.Assertions
import org.awaitility.Awaitility
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import traversium.notification.db.repository.UnseenNotificationRepository
import traversium.notification.security.MockFirebaseConfig
import traversium.notification.security.TestMultitenancyConfig
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import kotlin.test.Test

/**
 * @author Maja Razinger
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@SpringBootTest
@ExtendWith(SpringExtension::class)
@EmbeddedKafka(partitions = 1, topics = ["test-notification-topic"], bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@ActiveProfiles("test")
@ContextConfiguration(classes = [TestMultitenancyConfig::class, MockFirebaseConfig::class])
class KafkaConsumerTests(
) {

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var unseenNotificationRepository: UnseenNotificationRepository

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun testKafkaConsumer() {
        val testData = NotificationStreamData(
            timestamp = OffsetDateTime.now(),
            senderId = "sender1",
            receiverIds = listOf("receiver1", "receiver2"),
            collectionReferenceId = 123L,
            nodeReferenceId = null,
            mediaReferenceId = null,
            commentReferenceId = null,
            action = ActionType.CREATE
        )

        val json = ObjectMapper().registerModule(JavaTimeModule()).writeValueAsString(testData)

        kafkaTemplate.send("test-notification-topic", "key1", json)

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
            unseenNotificationRepository.count() >= 1
        }

        val savedNotification = unseenNotificationRepository.findAll().first()
        Assertions.assertThat(savedNotification.senderId).isEqualTo("sender1")
        Assertions.assertThat(savedNotification.receiverId).isEqualTo("receiver1")
        Assertions.assertThat(savedNotification.collectionReferenceId).isEqualTo(123L)

        unseenNotificationRepository.delete(savedNotification)
    }
}