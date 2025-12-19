package traversium.notification.db.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import traversium.notification.mapper.NotificationType
import java.time.OffsetDateTime

/**
 * @author Maja Razinger
 */
@Entity
@Table(name = SeenNotificationBundle.TABLE_NAME)
data class SeenNotificationBundle(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uuid", unique = true, nullable = false, updatable = false)
    val uuid: Long? = null,

    @Column(name = "bundle_id", nullable = false, updatable = false, length = 500)
    val bundleId: String? = null,

    @Column(name = "receiver_id", nullable = false)
    val receiverId: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sender_ids", nullable = false)
    val senderIds: List<String> = emptyList(),

    @Column(name = "action", nullable = false)
    val action: NotificationType = NotificationType.UNKNOWN,

    @Column(name = "collection_reference_id", nullable = true)
    val collectionReferenceId: Long? = null,

    @Column(name = "node_reference_id", nullable = true)
    val nodeReferenceId: Long? = null,

    @Column(name = "media_reference_id", nullable = true)
    val mediaReferenceId: Long? = null,

    @Column(name = "media_count", nullable = true)
    val mediaCount: Int? = null,

    @Column(name = "comment_reference_id", nullable = true)
    val commentReferenceId: Long? = null,

    @Column(name = "notification_count", nullable = false)
    val notificationCount: Int = 1,

    @Column(name = "first_timestamp", nullable = false)
    val firstTimestamp: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "last_timestamp", nullable = false)
    val lastTimestamp: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
) {
    companion object {
        const val TABLE_NAME = "seen_notification_bundle"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SeenNotificationBundle

        if (bundleId != other.bundleId) return false

        return true
    }

    override fun hashCode(): Int {
        return bundleId.hashCode()
    }
}
