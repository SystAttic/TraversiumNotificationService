package traversium.notification.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Sinks
import reactor.util.concurrent.Queues
import traversium.notification.dto.BundleIdDto

/**
 * @author Maja Razinger
 */
@Configuration
class ApplicationConfiguration {

    @Bean
    fun bundleIdSink() : Sinks.Many<BundleIdDto> =
        Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false)
}