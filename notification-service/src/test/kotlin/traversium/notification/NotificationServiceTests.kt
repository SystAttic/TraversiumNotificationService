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
import traversium.notification.db.model.Notification
import traversium.notification.db.repository.NotificationRepository
import traversium.notification.mapper.NotificationType
import traversium.notification.service.NotificationService
import travesium.userservice.security.MockFirebaseConfig
import kotlin.test.Test

/**
 * @author Maja Razinger
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@SpringBootTest(classes = [NotificationServiceApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
@Transactional
@DirtiesContext
@ContextConfiguration(classes = [MockFirebaseConfig::class])
class NotificationServiceTests@Autowired constructor(
    @Autowired val notificationService: NotificationService,
    @Autowired private val firebaseConfig: MockFirebaseConfig,
    private val notificationRepository: NotificationRepository
)
{
    @BeforeEach
    fun setup() {
        notificationRepository.deleteAll()

        for (i in 1..3) {
            notificationRepository.save(
                Notification(
                    senderId = "senderId$i",
                    receiverId = "user1",
                    collectionReferenceId = 1,
                    action = NotificationType.CREATE_COLLECTION
                )
            )
        }

        for (i in 1..2) {
            notificationRepository.save(
                Notification(
                    senderId = "senderId$i",
                    receiverId = "user1",
                    collectionReferenceId = 1,
                    action = NotificationType.UPDATE_COLLECTION,
                    seen = true
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
        val notifications = notificationService.getNotificationsForUser(0, 10)

        assert(notifications.size == 5)
    }
}