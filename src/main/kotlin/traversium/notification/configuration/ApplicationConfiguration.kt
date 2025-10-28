package traversium.notification.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Sinks
import reactor.util.concurrent.Queues
import traversium.notification.dto.NotificationDto

/**
 * @author Maja Razinger
 */
@Configuration
class ApplicationConfiguration {

    @Bean
    fun notificationSink() : Sinks.Many<NotificationDto> =
        Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false)
}