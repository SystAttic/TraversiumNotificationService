package traversium.notification.db.repository

import org.springframework.data.jpa.repository.JpaRepository
import traversium.notification.db.model.Notification

/**
 * @author Maja Razinger
 */

interface NotificationRepository : JpaRepository<Notification, String> {

}
