package traversium.notification.db.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

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

    @Column(name = "content", nullable = false)
    val content: String? = null,

    @Column(name = "reference_id")
    val referenceId: Long? = null
    ) {
    companion object {
        const val TABLE_NAME = "notification_table"
    }
}