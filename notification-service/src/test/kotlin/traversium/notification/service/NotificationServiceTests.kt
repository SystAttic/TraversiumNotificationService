package traversium.notification

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import traversium.notification.db.model.SeenNotificationBundle
import traversium.notification.db.model.UnseenNotification
import traversium.notification.db.repository.SeenNotificationBundleRepository
import traversium.notification.db.repository.UnseenNotificationRepository
import traversium.notification.mapper.NotificationType
import traversium.notification.security.MockFirebaseConfig
import traversium.notification.security.TestMultitenancyConfig
import traversium.notification.service.NotificationService
import java.time.OffsetDateTime
import kotlin.test.Test

/**
 * @author Maja Razinger
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@SpringBootTest(
    classes = [NotificationServiceApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ExtendWith(SpringExtension::class)
@Transactional
@DirtiesContext
@ContextConfiguration(classes = [MockFirebaseConfig::class, TestMultitenancyConfig::class])
class NotificationServiceTests @Autowired constructor(
    @Autowired val notificationService: NotificationService,
    @Autowired private val firebaseConfig: MockFirebaseConfig,
    private val unseenNotificationRepository: UnseenNotificationRepository,
    private val seenNotificationBundleRepository: SeenNotificationBundleRepository
)
{
    @BeforeEach
    fun setup() {
        unseenNotificationRepository.deleteAll()
        seenNotificationBundleRepository.deleteAll()

        for (i in 1..3) {
            unseenNotificationRepository.save(
                UnseenNotification(
                    senderId = "senderId$i",
                    receiverId = "user1",
                    collectionReferenceId = 1,
                    action = NotificationType.CREATE_TRIP,
                    timestamp = OffsetDateTime.now()
                )
            )
        }

        for (i in 1..2) {
            seenNotificationBundleRepository.save(
                SeenNotificationBundle(
                    bundleId = "user1-1-CHANGE_TRIP_TITLE-$i",
                    receiverId = "user1",
                    senderIds = listOf("senderId$i"),
                    collectionReferenceId = 1,
                    action = NotificationType.CHANGE_TRIP_TITLE,
                    notificationCount = 1,
                    firstTimestamp = OffsetDateTime.now().minusHours(1),
                    lastTimestamp = OffsetDateTime.now().minusHours(1)
                )
            )
        }

        firebaseConfig.setTokenData("token1", "user1", "mail")
        val auth = UsernamePasswordAuthenticationToken("principal", "token1")
        SecurityContextHolder.getContext().authentication = auth
    }

    @Test
    fun getUnseenNotificationsCount() {
        val unseenCount = notificationService.getUnseenNotificationsCount()

        assert(unseenCount.toInt() == 3)
    }

    @Test
    fun getAllNotificationsForUser() {
        val result = notificationService.getNotificationsForUser(0, 10)

        assert(result.unseenBundles.size == 3)
        assert(result.seenBundles.size == 2)
        assert(result.unseenBundles.size + result.seenBundles.size == 5)
    }
}