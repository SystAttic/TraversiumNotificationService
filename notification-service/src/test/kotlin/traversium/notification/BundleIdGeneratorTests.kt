package traversium.notification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import traversium.notification.db.model.UnseenNotification
import traversium.notification.mapper.NotificationType
import traversium.notification.util.BundleIdGenerator
import java.time.OffsetDateTime

/**
 * @author Maja Razinger
 */
class BundleIdGeneratorTests {

    @Test
    fun generateBundleForLikePhoto() {
        val notification = UnseenNotification(
            senderId = "sender1",
            receiverId = "recipient1",
            mediaReferenceId =123L,
            action = NotificationType.LIKE_PHOTO,
            timestamp = OffsetDateTime.now()
        )

        val bundleId = BundleIdGenerator.generateBundleId(notification)

        assertEquals("recipient1-123-LIKE_PHOTO", bundleId)
    }

    @Test
    fun generateBundleForAddPhotoIncludesSender() {
        val notification = UnseenNotification(
            senderId = "sender1",
            receiverId = "recipient1",
            collectionReferenceId = 456L,
            nodeReferenceId = 789L,
            action = NotificationType.ADD_PHOTO,
            timestamp = OffsetDateTime.now()
        )

        val bundleId = BundleIdGenerator.generateBundleId(notification)

        assertEquals("recipient1-sender1-456-789-ADD_PHOTO", bundleId)
    }

    @Test
    fun generateBundleForAddPhotoWithDifferentSendersCreatesDifferentBundles() {
        val notification1 = UnseenNotification(
            senderId = "alice",
            receiverId = "recipient1",
            collectionReferenceId = 456L,
            nodeReferenceId = 789L,
            action = NotificationType.ADD_PHOTO,
            timestamp = OffsetDateTime.now()
        )

        val notification2 = UnseenNotification(
            senderId = "bob",
            receiverId = "recipient1",
            collectionReferenceId = 456L,
            nodeReferenceId = 789L,
            action = NotificationType.ADD_PHOTO,
            timestamp = OffsetDateTime.now()
        )

        val bundleId1 = BundleIdGenerator.generateBundleId(notification1)
        val bundleId2 = BundleIdGenerator.generateBundleId(notification2)

        assertEquals("recipient1-alice-456-789-ADD_PHOTO", bundleId1)
        assertEquals("recipient1-bob-456-789-ADD_PHOTO", bundleId2)
        assert(bundleId1 != bundleId2)
    }

    @Test
    fun generateBundleForRemovePhotoIncludesSender() {
        val notification = UnseenNotification(
            senderId = "sender1",
            receiverId = "recipient1",
            collectionReferenceId = 456L,
            nodeReferenceId = 789L,
            action = NotificationType.REMOVE_PHOTO,
            timestamp = OffsetDateTime.now()
        )

        val bundleId = BundleIdGenerator.generateBundleId(notification)

        assertEquals("recipient1-sender1-456-789-REMOVE_PHOTO", bundleId)
    }

    @Test
    fun generateBundleForCreateMomentIncludesSender() {
        val notification = UnseenNotification(
            senderId = "sender1",
            receiverId = "recipient1",
            collectionReferenceId = 456L,
            nodeReferenceId = 789L,
            action = NotificationType.CREATE_MOMENT,
            timestamp = OffsetDateTime.now()
        )

        val bundleId = BundleIdGenerator.generateBundleId(notification)

        assertEquals("recipient1-sender1-456-789-CREATE_MOMENT", bundleId)
    }

    @Test
    fun generateBundleForDeleteMomentIncludesSender() {
        val notification = UnseenNotification(
            senderId = "sender1",
            receiverId = "recipient1",
            collectionReferenceId = 456L,
            nodeReferenceId = 789L,
            action = NotificationType.DELETE_MOMENT,
            timestamp = OffsetDateTime.now()
        )

        val bundleId = BundleIdGenerator.generateBundleId(notification)

        assertEquals("recipient1-sender1-456-789-DELETE_MOMENT", bundleId)
    }

    @Test
    fun generateBundleForChangeMomentTitleDoesNotIncludeSender() {
        val notification = UnseenNotification(
            senderId = "sender1",
            receiverId = "recipient1",
            collectionReferenceId = 456L,
            nodeReferenceId = 789L,
            action = NotificationType.CHANGE_MOMENT_TITLE,
            timestamp = OffsetDateTime.now()
        )

        val bundleId = BundleIdGenerator.generateBundleId(notification)

        // Should NOT include sender1 in the ID
        assertEquals("recipient1-456-789-CHANGE_MOMENT_TITLE", bundleId)
    }

    @Test
    fun generateBundleForChangeMomentCoverPhotoDoesNotIncludeSender() {
        val notification = UnseenNotification(
            senderId = "sender1",
            receiverId = "recipient1",
            collectionReferenceId = 456L,
            nodeReferenceId = 789L,
            action = NotificationType.CHANGE_MOMENT_COVER_PHOTO,
            timestamp = OffsetDateTime.now()
        )

        val bundleId = BundleIdGenerator.generateBundleId(notification)

        assertEquals("recipient1-456-789-CHANGE_MOMENT_COVER_PHOTO", bundleId)
    }

    @Test
    fun generateBundleForAddCommentGroupsByMediaAndComment() {
        val notification = UnseenNotification(
            senderId = "sender1",
            receiverId = "recipient1",
            mediaReferenceId = 123L,
            commentReferenceId = 456L,
            action = NotificationType.ADD_COMMENT,
            timestamp = OffsetDateTime.now()
        )

        val bundleId = BundleIdGenerator.generateBundleId(notification)

        assertEquals("recipient1-123-456-ADD_COMMENT", bundleId)
    }

    @Test
    fun generateBundleForReplyCommentGroupsByMediaAndComment() {
        val notification = UnseenNotification(
            senderId = "sender1",
            receiverId = "recipient1",
            mediaReferenceId = 123,
            commentReferenceId = 456L,
            action = NotificationType.REPLY_COMMENT,
            timestamp = OffsetDateTime.now()
        )

        val bundleId = BundleIdGenerator.generateBundleId(notification)

        assertEquals("recipient1-123-456-REPLY_COMMENT", bundleId)
    }

    @Test
    fun generateBundleForCreateTripGroupsByCollection() {
        val notification = UnseenNotification(
            senderId = "sender1",
            receiverId = "recipient1",
            collectionReferenceId = 789L,
            action = NotificationType.CREATE_TRIP,
            timestamp = OffsetDateTime.now()
        )

        val bundleId = BundleIdGenerator.generateBundleId(notification)

        assertEquals("recipient1-789-CREATE_TRIP", bundleId)
    }

    @Test
    fun generateBundleForAddCollaboratorGroupsByCollection() {
        val notification = UnseenNotification(
            senderId = "sender1",
            receiverId = "recipient1",
            collectionReferenceId = 789L,
            action = NotificationType.ADD_COLLABORATOR,
            timestamp = OffsetDateTime.now()
        )

        val bundleId = BundleIdGenerator.generateBundleId(notification)

        assertEquals("recipient1-789-ADD_COLLABORATOR", bundleId)
    }

    @Test
    fun generateBundleForFollowUserGroupsByRecipientAndTypeOnly() {
        val notification = UnseenNotification(
            senderId = "sender1",
            receiverId = "recipient1",
            action = NotificationType.FOLLOW_USER,
            timestamp = OffsetDateTime.now()
        )

        val bundleId = BundleIdGenerator.generateBundleId(notification)

        assertEquals("recipient1-FOLLOW_USER", bundleId)
    }

    @Test
    fun generateBundleThrowsExceptionForHealthcheck() {
        val notification = UnseenNotification(
            senderId = "sender1",
            receiverId = "recipient1",
            action = NotificationType.HEALTHCHECK,
            timestamp = OffsetDateTime.now()
        )

        assertThrows(IllegalArgumentException::class.java) {
            BundleIdGenerator.generateBundleId(notification)
        }
    }

    @Test
    fun generateBundleThrowsExceptionWhenReceiverIdIsNull() {
        val notification = UnseenNotification(
            senderId = "sender1",
            receiverId = null,
            action = NotificationType.FOLLOW_USER,
            timestamp = OffsetDateTime.now()
        )

        assertThrows(IllegalArgumentException::class.java) {
            BundleIdGenerator.generateBundleId(notification)
        }
    }

    @Test
    fun generateBundleThrowsExceptionWhenSenderIdIsNull() {
        val notification = UnseenNotification(
            senderId = null,
            receiverId = "recipient1",
            action = NotificationType.FOLLOW_USER,
            timestamp = OffsetDateTime.now()
        )

        assertThrows(IllegalArgumentException::class.java) {
            BundleIdGenerator.generateBundleId(notification)
        }
    }

    @Test
    fun generateBundleThrowsExceptionWhenActionIsNull() {
        val notification = UnseenNotification(
            senderId = "sender1",
            receiverId = "recipient1",
            action = null,
            timestamp = OffsetDateTime.now()
        )

        assertThrows(IllegalArgumentException::class.java) {
            BundleIdGenerator.generateBundleId(notification)
        }
    }

    @Test
    fun generateBundleHandlesNullOptionalReferenceIdsCorrectly() {
        val notification = UnseenNotification(
            senderId = "sender1",
            receiverId = "recipient1",
            collectionReferenceId = null,
            nodeReferenceId = null,
            mediaReferenceId = null,
            commentReferenceId = null,
            action = NotificationType.FOLLOW_USER,
            timestamp = OffsetDateTime.now()
        )

        val bundleId = BundleIdGenerator.generateBundleId(notification)

        assertEquals("recipient1-FOLLOW_USER", bundleId)
    }
}
