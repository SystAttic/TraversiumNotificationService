package traversium.notification.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import traversium.notification.NotificationServiceApplication
import traversium.notification.db.model.UnseenNotification
import traversium.notification.db.repository.SeenNotificationBundleRepository
import traversium.notification.db.repository.UnseenNotificationRepository
import traversium.notification.mapper.NotificationType
import traversium.notification.security.MockFirebaseConfig
import traversium.notification.security.TestMultitenancyConfig
import java.time.OffsetDateTime

/**
 * @author Maja Razinger
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@SpringBootTest(
    classes = [
        NotificationServiceApplication::class,
        MockFirebaseConfig::class,
        TestMultitenancyConfig::class,
        NotificationServiceApplication::class
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Transactional
@DirtiesContext
class NotificationBundlingTests @Autowired constructor(
    private val notificationService: NotificationService,
    private val unseenNotificationRepository: UnseenNotificationRepository,
    private val seenNotificationBundleRepository: SeenNotificationBundleRepository,
    private val firebaseConfig: MockFirebaseConfig
) {

    @BeforeEach
    fun setup() {
        unseenNotificationRepository.deleteAll()
        seenNotificationBundleRepository.deleteAll()

        firebaseConfig.setTokenData("token1", "user1", "mail")
        val auth = UsernamePasswordAuthenticationToken("principal", "token1")
        SecurityContextHolder.getContext().authentication = auth
    }

    @Test
    fun getUnseenNotificationsCountReturnsCorrectCount() {
        repeat(3) {
            unseenNotificationRepository.save(
                UnseenNotification(
                    senderId = "sender$it",
                    receiverId = "user1",
                    collectionReferenceId = 1L,
                    action = NotificationType.CREATE_TRIP,
                    timestamp = OffsetDateTime.now()
                )
            )
        }

        val count = notificationService.getUnseenNotificationsCount()

        assertEquals(3L, count)
    }

    @Test
    fun createBundlesFromUnseenCreatesSingleBundleFromMultipleNotificationsWithSameBundleId() {
        val notifications = listOf(
            UnseenNotification(
                senderId = "alice",
                receiverId = "user1",
                collectionReferenceId = 123L,
                nodeReferenceId = 456L,
                mediaReferenceId = 1L,
                action = NotificationType.ADD_PHOTO,
                timestamp = OffsetDateTime.now()
            ),
            UnseenNotification(
                senderId = "alice",
                receiverId = "user1",
                collectionReferenceId = 123L,
                nodeReferenceId = 456L,
                mediaReferenceId = 2L,
                action = NotificationType.ADD_PHOTO,
                timestamp = OffsetDateTime.now()
            ),
            UnseenNotification(
                senderId = "alice",
                receiverId = "user1",
                collectionReferenceId = 123L,
                nodeReferenceId = 456L,
                mediaReferenceId = 3L,
                action = NotificationType.ADD_PHOTO,
                timestamp = OffsetDateTime.now()
            )
        ).map { unseenNotificationRepository.save(it) }

        notificationService.createBundlesFromUnseen(notifications)

        val bundles = seenNotificationBundleRepository.findAll()
        assertEquals(1, bundles.size)

        val bundle = bundles[0]
        assertEquals("user1-alice-123-456-ADD_PHOTO", bundle.bundleId)
        assertEquals(1, bundle.senderIds.size)
        assertEquals("alice", bundle.senderIds[0])
        assertEquals(1L, bundle.mediaReferenceId)
        assertEquals(3, bundle.mediaCount)
        assertEquals(3, bundle.notificationCount)
        assertEquals(NotificationType.ADD_PHOTO, bundle.action)

        assertEquals(0, unseenNotificationRepository.count())
    }

    @Test
    fun createBundlesFromUnseenCreatesSeparateBundlesForDifferentSenders() {
        val notifications = listOf(
            UnseenNotification(
                senderId = "alice",
                receiverId = "user1",
                collectionReferenceId = 123L,
                nodeReferenceId = 456L,
                mediaReferenceId = 1L,
                action = NotificationType.ADD_PHOTO,
                timestamp = OffsetDateTime.now()
            ),
            UnseenNotification(
                senderId = "alice",
                receiverId = "user1",
                collectionReferenceId = 123L,
                nodeReferenceId = 456L,
                mediaReferenceId = 2L,
                action = NotificationType.ADD_PHOTO,
                timestamp = OffsetDateTime.now()
            ),
            UnseenNotification(
                senderId = "bob",
                receiverId = "user1",
                collectionReferenceId = 123L,
                nodeReferenceId = 456L,
                mediaReferenceId = 3L,
                action = NotificationType.ADD_PHOTO,
                timestamp = OffsetDateTime.now()
            ),
            UnseenNotification(
                senderId = "bob",
                receiverId = "user1",
                collectionReferenceId = 123L,
                nodeReferenceId = 456L,
                mediaReferenceId = 4L,
                action = NotificationType.ADD_PHOTO,
                timestamp = OffsetDateTime.now()
            )
        ).map { unseenNotificationRepository.save(it) }

        notificationService.createBundlesFromUnseen(notifications)

        val bundles = seenNotificationBundleRepository.findAll()
        assertEquals(2, bundles.size)

        val aliceBundle = bundles.find { it.bundleId?.contains("alice") == true }
        val bobBundle = bundles.find { it.bundleId?.contains("bob") == true }

        assertEquals("user1-alice-123-456-ADD_PHOTO", aliceBundle?.bundleId)
        assertEquals(2, aliceBundle?.notificationCount)
        assertEquals(2, aliceBundle?.mediaCount)

        assertEquals("user1-bob-123-456-ADD_PHOTO", bobBundle?.bundleId)
        assertEquals(2, bobBundle?.notificationCount)
        assertEquals(2, bobBundle?.mediaCount)
    }

    @Test
    fun createBundlesFromUnseenUpdatesExistingBundleWhenCalledMultipleTimes() {
        val initialNotifications = listOf(
            UnseenNotification(
                senderId = "alice",
                receiverId = "user1",
                collectionReferenceId = 123L,
                nodeReferenceId = 456L,
                mediaReferenceId = 1L,
                action = NotificationType.ADD_PHOTO,
                timestamp = OffsetDateTime.now()
            ),
            UnseenNotification(
                senderId = "alice",
                receiverId = "user1",
                collectionReferenceId = 123L,
                nodeReferenceId = 456L,
                mediaReferenceId = 2L,
                action = NotificationType.ADD_PHOTO,
                timestamp = OffsetDateTime.now()
            )
        ).map { unseenNotificationRepository.save(it) }

        notificationService.createBundlesFromUnseen(initialNotifications)

        val newNotifications = listOf(
            UnseenNotification(
                senderId = "alice",
                receiverId = "user1",
                collectionReferenceId = 123L,
                nodeReferenceId = 456L,
                mediaReferenceId = 3L,
                action = NotificationType.ADD_PHOTO,
                timestamp = OffsetDateTime.now()
            ),
            UnseenNotification(
                senderId = "bob",
                receiverId = "user1",
                collectionReferenceId = 123L,
                nodeReferenceId = 456L,
                mediaReferenceId = 4L,
                action = NotificationType.ADD_PHOTO,
                timestamp = OffsetDateTime.now()
            )
        ).map { unseenNotificationRepository.save(it) }

        notificationService.createBundlesFromUnseen(newNotifications)

        val bundles = seenNotificationBundleRepository.findAll()
        assertEquals(3, bundles.size)

        val aliceBundle = bundles.find { it.bundleId?.contains("alice") == true }
        assertEquals(2, aliceBundle?.notificationCount)
        assertEquals(2, aliceBundle?.mediaCount)
        assertEquals(1, aliceBundle?.senderIds?.size)
    }

    @Test
    fun createBundlesFromUnseenHandlesMultipleSendersInSameBundleForNonSenderSpecificTypes() {
        val notifications = listOf(
            UnseenNotification(
                senderId = "alice",
                receiverId = "user1",
                mediaReferenceId = 789L,
                action = NotificationType.LIKE_PHOTO,
                timestamp = OffsetDateTime.now()
            ),
            UnseenNotification(
                senderId = "bob",
                receiverId = "user1",
                mediaReferenceId = 789L,
                action = NotificationType.LIKE_PHOTO,
                timestamp = OffsetDateTime.now()
            ),
            UnseenNotification(
                senderId = "charlie",
                receiverId = "user1",
                mediaReferenceId = 789L,
                action = NotificationType.LIKE_PHOTO,
                timestamp = OffsetDateTime.now()
            )
        ).map { unseenNotificationRepository.save(it) }

        notificationService.createBundlesFromUnseen(notifications)

        val bundles = seenNotificationBundleRepository.findAll()
        assertEquals(1, bundles.size)

        val bundle = bundles[0]
        assertEquals("user1-789-LIKE_PHOTO", bundle.bundleId)
        assertEquals(3, bundle.senderIds.size)
        assertTrue(bundle.senderIds.contains("alice"))
        assertTrue(bundle.senderIds.contains("bob"))
        assertTrue(bundle.senderIds.contains("charlie"))
        assertEquals(3, bundle.notificationCount)
    }

    @Test
    fun getNotificationsForUserCreatesBundlesAndReturnsPaginatedResults() {
        unseenNotificationRepository.saveAll(
            listOf(
                UnseenNotification(
                    senderId = "alice",
                    receiverId = "user1",
                    collectionReferenceId = 123L,
                    nodeReferenceId = 456L,
                    mediaReferenceId = 1L,
                    action = NotificationType.ADD_PHOTO,
                    timestamp = OffsetDateTime.now()
                ),
                UnseenNotification(
                    senderId = "alice",
                    receiverId = "user1",
                    collectionReferenceId = 123L,
                    nodeReferenceId = 456L,
                    mediaReferenceId = 2L,
                    action = NotificationType.ADD_PHOTO,
                    timestamp = OffsetDateTime.now()
                ),
                UnseenNotification(
                    senderId = "bob",
                    receiverId = "user1",
                    mediaReferenceId = 789L,
                    action = NotificationType.LIKE_PHOTO,
                    timestamp = OffsetDateTime.now()
                ),
                UnseenNotification(
                    senderId = "charlie",
                    receiverId = "user1",
                    action = NotificationType.FOLLOW_USER,
                    timestamp = OffsetDateTime.now()
                ),
                UnseenNotification(
                    senderId = "dave",
                    receiverId = "user1",
                    action = NotificationType.FOLLOW_USER,
                    timestamp = OffsetDateTime.now()
                )
            )
        )

        val result = notificationService.getNotificationsForUser(0, 10)

        assertEquals(3, result.unseenBundles.size + result.seenBundles.size)
        assertEquals(0, unseenNotificationRepository.count())
        assertEquals(3, seenNotificationBundleRepository.count())
    }

    @Test
    fun createBundlesFromUnseenCorrectlyAggregatesMediaIds() {
        val notifications = listOf(
            UnseenNotification(
                senderId = "alice",
                receiverId = "user1",
                collectionReferenceId = 123L,
                nodeReferenceId = 456L,
                mediaReferenceId = 10L,
                action = NotificationType.ADD_PHOTO,
                timestamp = OffsetDateTime.now()
            ),
            UnseenNotification(
                senderId = "alice",
                receiverId = "user1",
                collectionReferenceId = 123L,
                nodeReferenceId = 456L,
                mediaReferenceId = 20L,
                action = NotificationType.ADD_PHOTO,
                timestamp = OffsetDateTime.now()
            ),
            UnseenNotification(
                senderId = "alice",
                receiverId = "user1",
                collectionReferenceId = 123L,
                nodeReferenceId = 456L,
                mediaReferenceId = 30L,
                action = NotificationType.ADD_PHOTO,
                timestamp = OffsetDateTime.now()
            )
        ).map { unseenNotificationRepository.save(it) }

        notificationService.createBundlesFromUnseen(notifications)

        val bundle = seenNotificationBundleRepository.findAll()[0]
        assertEquals(10L, bundle.mediaReferenceId)
        assertEquals(3, bundle.mediaCount)
    }

    @Test
    fun createBundlesFromUnseenHandlesNotificationsWithoutMediaIds() {
        val notifications = listOf(
            UnseenNotification(
                senderId = "alice",
                receiverId = "user1",
                action = NotificationType.FOLLOW_USER,
                timestamp = OffsetDateTime.now()
            ),
            UnseenNotification(
                senderId = "bob",
                receiverId = "user1",
                action = NotificationType.FOLLOW_USER,
                timestamp = OffsetDateTime.now()
            )
        ).map { unseenNotificationRepository.save(it) }

        notificationService.createBundlesFromUnseen(notifications)

        val bundle = seenNotificationBundleRepository.findAll()[0]
        assertEquals(null, bundle.mediaReferenceId)
        assertEquals(null, bundle.mediaCount)
        assertEquals(2, bundle.senderIds.size)
        assertEquals(2, bundle.notificationCount)
    }
}
