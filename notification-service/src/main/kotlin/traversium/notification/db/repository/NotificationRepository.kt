package traversium.notification.db.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import traversium.notification.db.model.Notification

/**
 * @author Maja Razinger
 */

interface NotificationRepository : JpaRepository<Notification, String> {

    fun countByReceiverIdAndSeenFalse(receiverId: String): Long

    fun findByReceiverId(receiverId: String, pageable: Pageable): Page<Notification>

}
