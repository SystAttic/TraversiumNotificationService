package traversium.notification.rest

import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.BufferOverflowStrategy
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import reactor.util.concurrent.Queues
import traversium.notification.dto.NotificationDto
import traversium.notification.mapper.NotificationType
import traversium.notification.service.NotificationService
import java.util.*

/**
 * @author Maja Razinger
 */
@RestController
@RequestMapping(path = ["/rest/v1/notifications"])
class NotificationController(
    private val notificationSink: Sinks.Many<NotificationDto>,
    private val notificationService: NotificationService
) {

    @GetMapping("/sse/{userId}")
    fun sse(@PathVariable("userId") userId: String): Flux<ServerSentEvent<NotificationDto>>? {
        return notificationSink.asFlux()
            .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, BufferOverflowStrategy.DROP_OLDEST)
            .publishOn(Schedulers.boundedElastic())
            .filter { it.recipientId == userId }
            .map { notificationDto ->
                ServerSentEvent.builder(notificationDto)
                    .id(UUID.randomUUID().toString())
                    .event(if (notificationDto.type == NotificationType.HEALTHCHECK) "heartbeat" else "Notification")
                    .data(notificationDto)
                    .build()
            }
    }

    @GetMapping("/unseen")
    fun getUnseenNotificationsCountForUser(): Long =
        notificationService.getUnseenNotificationsCount()

    @GetMapping
    fun getNotificationsForUser(
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): List<NotificationDto> =
        notificationService.getNotificationsForUser(offset, limit)
}