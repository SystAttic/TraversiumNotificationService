package traversium.notification.db.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import traversium.notification.db.model.SeenNotificationBundle

/**
 * @author Maja Razinger
 */
interface SeenNotificationBundleRepository : JpaRepository<SeenNotificationBundle, String> {

    fun findByReceiverId(receiverId: String, pageable: Pageable): Page<SeenNotificationBundle>

    fun findByBundleId(bundleId: String): SeenNotificationBundle?
}
