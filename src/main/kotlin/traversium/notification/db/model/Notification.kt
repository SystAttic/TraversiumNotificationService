package traversium.notification.db.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

/**
 * @author Maja Razinger
 */
@Entity
@Table(name = Notification.TABLE_NAME)
class Notification(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", unique = true, nullable = false, updatable = false, length = 36)
    val notificationId: Long? = null,

    @Column(name = "sender_id", nullable = false)
    val senderId: String? = null,

    @Column(name = "receiver_ids", nullable = false)
    val receiverIds: List<String> = emptyList(),

    @Column(name = "collection_reference_id", nullable = true)
    val collectionReferenceId: Long? = null,

    @Column(name = "node_reference_id", nullable = true)
    val nodeReferenceId: Long? = null,

    @Column(name = "comment_reference_id", nullable = true)
    val commentReferenceId: Long? = null,

    @Column(name = "action")
    val action: String? = null,

    @Column(name = "timestamp")
    val timestamp: OffsetDateTime? = OffsetDateTime.now(),

    ) {
    companion object {
        const val TABLE_NAME = "notification_table"
    }
}