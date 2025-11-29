package traversium.notification.db.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import traversium.notification.db.model.UnseenNotification
import traversium.notification.mapper.NotificationType

/**
 * @author Maja Razinger
 */
interface UnseenNotificationRepository : JpaRepository<UnseenNotification, Long> {

    fun countByReceiverId(receiverId: String): Long

    fun deleteByReceiverId(receiverId: String)

    fun findByReceiverId(receiverId: String, pageable: Pageable): Page<UnseenNotification>

    @Query("""
        SELECT n FROM UnseenNotification n
        WHERE n.receiverId = :receiverId
        AND n.action = :action
        AND COALESCE(n.collectionReferenceId, -1) = COALESCE(:collectionId, -1)
        AND COALESCE(n.nodeReferenceId, -1) = COALESCE(:nodeId, -1)
        AND COALESCE(n.commentReferenceId, -1) = COALESCE(:commentId, -1)
        ORDER BY n.timestamp ASC
    """)
    fun findNotificationsForBundle(
        @Param("receiverId") receiverId: String,
        @Param("action") action: NotificationType,
        @Param("collectionId") collectionId: Long?,
        @Param("nodeId") nodeId: Long?,
        @Param("commentId") commentId: Long?
    ): List<UnseenNotification>

    @Query("""
        SELECT DISTINCT n.action, n.receiverId, n.collectionReferenceId, n.nodeReferenceId, n.commentReferenceId
        FROM UnseenNotification n
    """)
    fun findDistinctBundleKeys(): List<Array<Any>>
}
