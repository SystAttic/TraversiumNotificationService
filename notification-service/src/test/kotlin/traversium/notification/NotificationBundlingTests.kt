package traversium.notification

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
import traversium.notification.db.model.UnseenNotification
import traversium.notification.db.repository.SeenNotificationBundleRepository
import traversium.notification.db.repository.UnseenNotificationRepository
import traversium.notification.mapper.NotificationType
import traversium.notification.security.MockFirebaseConfig
import traversium.notification.security.TestMultitenancyConfig
import traversium.notification.service.NotificationService
import java.time.OffsetDateTime

/**
 * Integration tests for notification bundling functionality
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
        // Given: 3 unseen notifications for user1
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

        // When: Getting unseen count
        val count = notificationService.getUnseenNotificationsCount()

        // Then: Should return 3
        assertEquals(3L, count)
    }

    @Test
    fun createBundlesFromUnseenCreatesSingleBundleFromMultipleNotificationsWithSameBundleId() {
        // Given: 3 notifications from same sender adding photos to same moment
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

        // When: Creating bundles
        notificationService.createBundlesFromUnseen(notifications)

        // Then: Should create 1 bundle
        val bundles = seenNotificationBundleRepository.findAll()
        assertEquals(1, bundles.size)

        val bundle = bundles[0]
        assertEquals("user1-alice-123-456-ADD_PHOTO", bundle.bundleId)
        assertEquals(1, bundle.senderIds.size)
        assertEquals("alice", bundle.senderIds[0])
        assertEquals(3, bundle.mediaReferenceIds?.size)
        assertEquals(3, bundle.notificationCount)
        assertEquals(NotificationType.ADD_PHOTO, bundle.action)

        // And: Unseen notifications should be deleted
        assertEquals(0, unseenNotificationRepository.count())
    }

    @Test
    fun createBundlesFromUnseenCreatesSeparateBundlesForDifferentSenders() {
        // Given: 4 notifications - 2 from alice, 2 from bob - adding photos to same moment
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

        // When: Creating bundles
        notificationService.createBundlesFromUnseen(notifications)

        // Then: Should create 2 bundles (one per sender)
        val bundles = seenNotificationBundleRepository.findAll()
        assertEquals(2, bundles.size)

        val aliceBundle = bundles.find { it.bundleId?.contains("alice") == true }
        val bobBundle = bundles.find { it.bundleId?.contains("bob") == true }

        assertEquals("user1-alice-123-456-ADD_PHOTO", aliceBundle?.bundleId)
        assertEquals(2, aliceBundle?.notificationCount)
        assertEquals(2, aliceBundle?.mediaReferenceIds?.size)

        assertEquals("user1-bob-123-456-ADD_PHOTO", bobBundle?.bundleId)
        assertEquals(2, bobBundle?.notificationCount)
        assertEquals(2, bobBundle?.mediaReferenceIds?.size)
    }

    @Test
    fun createBundlesFromUnseenUpdatesExistingBundleWhenCalledMultipleTimes() {
        // Given: Initial bundle with 2 notifications
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

        // When: Adding more notifications to the same bundle
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
                senderId = "bob",  // New sender to same bundle context
                receiverId = "user1",
                collectionReferenceId = 123L,
                nodeReferenceId = 456L,
                mediaReferenceId = 4L,
                action = NotificationType.ADD_PHOTO,
                timestamp = OffsetDateTime.now()
            )
        ).map { unseenNotificationRepository.save(it) }

        notificationService.createBundlesFromUnseen(newNotifications)

        // Then: Should have 2 bundles (alice's bundle updated, bob's bundle created)
        val bundles = seenNotificationBundleRepository.findAll()
        assertEquals(2, bundles.size)

        val aliceBundle = bundles.find { it.bundleId?.contains("alice") == true }
        assertEquals(3, aliceBundle?.notificationCount) // Updated from 2 to 3
        assertEquals(3, aliceBundle?.mediaReferenceIds?.size)
        assertEquals(1, aliceBundle?.senderIds?.size) // Still only alice
    }

    @Test
    fun createBundlesFromUnseenHandlesMultipleSendersInSameBundleForNonSenderSpecificTypes() {
        // Given: Multiple users liking the same photo (LIKE_PHOTO doesn't include sender in bundle ID)
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

        // When: Creating bundles
        notificationService.createBundlesFromUnseen(notifications)

        // Then: Should create 1 bundle with all 3 senders
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
        // Given: 5 unseen notifications from different contexts
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

        // When: Getting notifications for user
        val result = notificationService.getNotificationsForUser(0, 10)

        // Then: Should return 3 bundles (ADD_PHOTO, LIKE_PHOTO, FOLLOW_USER)
        assertEquals(3, result.unseenBundles.size + result.seenBundles.size)

        // And: Unseen table should be empty
        assertEquals(0, unseenNotificationRepository.count())

        // And: Seen table should have 3 bundles
        assertEquals(3, seenNotificationBundleRepository.count())
    }

    @Test
    fun getNotificationsForUserHandlesPaginationCorrectly() {
        // Given: 5 different bundles
        unseenNotificationRepository.saveAll(
            listOf(
                UnseenNotification(
                    senderId = "sender1",
                    receiverId = "user1",
                    collectionReferenceId = 1L,
                    action = NotificationType.CREATE_TRIP,
                    timestamp = OffsetDateTime.now().minusHours(5)
                ),
                UnseenNotification(
                    senderId = "sender2",
                    receiverId = "user1",
                    collectionReferenceId = 2L,
                    action = NotificationType.CREATE_TRIP,
                    timestamp = OffsetDateTime.now().minusHours(4)
                ),
                UnseenNotification(
                    senderId = "sender3",
                    receiverId = "user1",
                    collectionReferenceId = 3L,
                    action = NotificationType.CREATE_TRIP,
                    timestamp = OffsetDateTime.now().minusHours(3)
                ),
                UnseenNotification(
                    senderId = "sender4",
                    receiverId = "user1",
                    collectionReferenceId = 4L,
                    action = NotificationType.CREATE_TRIP,
                    timestamp = OffsetDateTime.now().minusHours(2)
                ),
                UnseenNotification(
                    senderId = "sender5",
                    receiverId = "user1",
                    collectionReferenceId = 5L,
                    action = NotificationType.CREATE_TRIP,
                    timestamp = OffsetDateTime.now().minusHours(1)
                )
            )
        )

        // When: Getting first page (limit 2)
        val firstPage = notificationService.getNotificationsForUser(0, 2)

        // Then: Should return 2 bundles
        assertEquals(2, firstPage.unseenBundles.size + firstPage.seenBundles.size)

        // When: Getting second page
        val secondPage = notificationService.getNotificationsForUser(2, 2)

        // Then: Should return 2 bundles
        assertEquals(2, secondPage.unseenBundles.size + secondPage.seenBundles.size)

        // When: Getting third page
        val thirdPage = notificationService.getNotificationsForUser(4, 2)

        // Then: Should return 1 bundle
        assertEquals(1, thirdPage.unseenBundles.size + thirdPage.seenBundles.size)
    }

    @Test
    fun createBundlesFromUnseenCorrectlyAggregatesMediaIds() {
        // Given: Notifications with different media IDs
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
        assertEquals(3, bundle.mediaReferenceIds?.size)
        assertTrue(bundle.mediaReferenceIds!!.contains(10L))
        assertTrue(bundle.mediaReferenceIds!!.contains(20L))
        assertTrue(bundle.mediaReferenceIds!!.contains(30L))
    }

    @Test
    fun createBundlesFromUnseenHandlesNotificationsWithoutMediaIds() {
        // Given: FOLLOW_USER notifications (no media IDs)
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

        // When: Creating bundles
        notificationService.createBundlesFromUnseen(notifications)

        // Then: Bundle should have empty media IDs array
        val bundle = seenNotificationBundleRepository.findAll()[0]
        assertTrue(bundle.mediaReferenceIds == null || bundle.mediaReferenceIds!!.isEmpty())
        assertEquals(2, bundle.senderIds.size)
        assertEquals(2, bundle.notificationCount)
    }
}
