package traversium.notification.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import traversium.notification.db.model.Notification
import traversium.notification.db.repository.NotificationRepository
import traversium.notification.kafka.KafkaStreamData
import traversium.notification.mapper.NotificationMapper.toEntity

/**
 * @author Maja Razinger
 */
@Service
class NotificationService(
    private val notificationRepository: NotificationRepository
) {
    @Transactional
    fun saveNotification(streamData: KafkaStreamData) : Notification {
        val entity = streamData.toEntity()
        return notificationRepository.save(entity)
    }
}