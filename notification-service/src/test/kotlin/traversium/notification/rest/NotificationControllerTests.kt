package traversium.notification.rest

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import traversium.notification.NotificationServiceApplication
import traversium.notification.db.model.SeenNotificationBundle
import traversium.notification.db.model.UnseenNotification
import traversium.notification.db.repository.SeenNotificationBundleRepository
import traversium.notification.db.repository.UnseenNotificationRepository
import traversium.notification.mapper.NotificationType
import traversium.notification.security.BaseSecuritySetup
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
        TestMultitenancyConfig::class
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@DirtiesContext
class NotificationControllerTests @Autowired constructor(
    private val mockMvc: MockMvc,
    private val unseenNotificationRepository: UnseenNotificationRepository,
    private val seenNotificationBundleRepository: SeenNotificationBundleRepository,
    private val firebaseConfig: MockFirebaseConfig
) : BaseSecuritySetup() {

    private fun MockMvc.performWithAuth(requestBuilder: MockHttpServletRequestBuilder): ResultActions =
        perform(requestBuilder.header("Authorization", "Bearer $token"))

    @BeforeEach
    fun setup() {
        unseenNotificationRepository.deleteAll()
        seenNotificationBundleRepository.deleteAll()
        firebaseConfig.setTokenData(token, firebaseId, email)
    }

    @Test
    fun countUnseenTest() {
        for (i in 1..3) {
            unseenNotificationRepository.save(
                UnseenNotification(
                    senderId = "sender$i",
                    receiverId = firebaseId,
                    collectionReferenceId = 123L,
                    action = NotificationType.CREATE_TRIP,
                    timestamp = OffsetDateTime.now()
                )
            )
        }

        mockMvc.performWithAuth(
            get("/rest/v1/notifications/unseen/count")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().string("3"))
    }

    @Test
    fun zeroUnseenCountTest() {
        mockMvc.performWithAuth(
            get("/rest/v1/notifications/unseen/count")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().string("0"))
    }

    @Test
    fun returnSeenAndUnseenBundle() {
        unseenNotificationRepository.save(
            UnseenNotification(
                senderId = "alice",
                receiverId = firebaseId,
                mediaReferenceId = 123L,
                action = NotificationType.LIKE_PHOTO,
                timestamp = OffsetDateTime.now()
            )
        )
        unseenNotificationRepository.save(
            UnseenNotification(
                senderId = "bob",
                receiverId = firebaseId,
                mediaReferenceId = 123L,
                action = NotificationType.LIKE_PHOTO,
                timestamp = OffsetDateTime.now()
            )
        )

        seenNotificationBundleRepository.save(
            SeenNotificationBundle(
                bundleId = "$firebaseId-456-LIKE_PHOTO",
                receiverId = firebaseId,
                senderIds = listOf("charlie"),
                action = NotificationType.LIKE_PHOTO,
                mediaReferenceIds = listOf(456L),
                notificationCount = 1,
                firstTimestamp = OffsetDateTime.now().minusHours(2),
                lastTimestamp = OffsetDateTime.now().minusHours(2)
            )
        )

        mockMvc.performWithAuth(
            get
            ("/rest/v1/notifications")
                .param("offset", "0")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.unseenBundles").isArray)
            .andExpect(jsonPath("$.unseenBundles.length()").value(1))
            .andExpect(jsonPath("$.unseenBundles[0].senderIds.length()").value(2))
            .andExpect(jsonPath("$.seenBundles").isArray)
            .andExpect(jsonPath("$.seenBundles.length()").value(1))
    }

    @Test
    fun paginationTest() {
        for (i in 1..5) {
            seenNotificationBundleRepository.save(
                SeenNotificationBundle(
                    bundleId = "$firebaseId-$i-LIKE_PHOTO",
                    receiverId = firebaseId,
                    senderIds = listOf("sender$i"),
                    action = NotificationType.LIKE_PHOTO,
                    mediaReferenceIds = listOf(i.toLong()),
                    notificationCount = 1,
                    firstTimestamp = OffsetDateTime.now().minusHours(i.toLong()),
                    lastTimestamp = OffsetDateTime.now().minusHours(i.toLong())
                )
            )
        }

        mockMvc.performWithAuth(
            get("/rest/v1/notifications")
                .param("offset", "0")
                .param("limit", "2")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.unseenBundles.length()").value(0))
            .andExpect(jsonPath("$.seenBundles.length()").value(2))
    }

    @Test
    fun getUnseenNotificationsTest() {
        for (i in 1..3) {
            unseenNotificationRepository.save(
                UnseenNotification(
                    senderId = "sender$i",
                    receiverId = firebaseId,
                    collectionReferenceId = 123L,
                    action = NotificationType.CREATE_TRIP,
                    timestamp = OffsetDateTime.now()
                )
            )
        }

        val unseenCountBefore = unseenNotificationRepository.count()

        mockMvc.performWithAuth(
            get("/rest/v1/notifications/unseen")
                .param("offset", "0")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].bundleId").value("$firebaseId-sender3-123-CREATE_TRIP"))
            .andExpect(jsonPath("$[0].senderIds.length()").value(1))
            .andExpect(jsonPath("$[0].notificationCount").value(1))

        assert(unseenCountBefore == 3L)
        assert(unseenNotificationRepository.count() == 0L)
        assert(seenNotificationBundleRepository.count() == 3L)
    }

    @Test
    fun zeroUnseenNotificationsTest() {
        mockMvc.performWithAuth(
            get("/rest/v1/notifications/unseen")
                .param("offset", "0")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun getSeenNotificationBundles() {
        seenNotificationBundleRepository.save(
            SeenNotificationBundle(
                bundleId = "$firebaseId-123-LIKE_PHOTO",
                receiverId = firebaseId,
                senderIds = listOf("alice"),
                action = NotificationType.LIKE_PHOTO,
                mediaReferenceIds = listOf(123L),
                notificationCount = 1,
                firstTimestamp = OffsetDateTime.now().minusHours(1),
                lastTimestamp = OffsetDateTime.now().minusHours(1)
            )
        )
        seenNotificationBundleRepository.save(
            SeenNotificationBundle(
                bundleId = "$firebaseId-456-LIKE_PHOTO",
                receiverId = firebaseId,
                senderIds = listOf("bob"),
                action = NotificationType.LIKE_PHOTO,
                mediaReferenceIds = listOf(456L),
                notificationCount = 1,
                firstTimestamp = OffsetDateTime.now().minusHours(2),
                lastTimestamp = OffsetDateTime.now().minusHours(2)
            )
        )

        mockMvc.performWithAuth(
            get("/rest/v1/notifications/seen")
                .param("offset", "0")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun getSeenNotificationBundlesWithPagination() {
        for (i in 1..5) {
            seenNotificationBundleRepository.save(
                SeenNotificationBundle(
                    bundleId = "$firebaseId-$i-LIKE_PHOTO",
                    receiverId = firebaseId,
                    senderIds = listOf("sender$i"),
                    action = NotificationType.LIKE_PHOTO,
                    mediaReferenceIds = listOf(i.toLong()),
                    notificationCount = 1,
                    firstTimestamp = OffsetDateTime.now().minusHours(i.toLong()),
                    lastTimestamp = OffsetDateTime.now().minusHours(i.toLong())
                )
            )
        }

        mockMvc.performWithAuth(
            get("/rest/v1/notifications/seen")
                .param("offset", "2")
                .param("limit", "2")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun getSeenNotificationBundleEmptyList() {
        mockMvc.performWithAuth(
            get("/rest/v1/notifications/seen")
                .param("offset", "0")
                .param("limit", "20")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))
    }
}
