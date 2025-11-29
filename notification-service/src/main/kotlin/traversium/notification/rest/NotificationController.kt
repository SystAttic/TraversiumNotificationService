package traversium.notification.rest

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import traversium.notification.dto.BundleIdDto
import traversium.notification.dto.NotificationBundleListDto
import traversium.notification.service.NotificationService

/**
 * @author Maja Razinger
 */
@RestController
@RequestMapping(path = ["/rest/v1/notifications"])
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping("/sse")
    @Operation(
        operationId = "sseNotifications",
        tags = ["Notifications"],
        summary = "SSE Notification Bundle IDs",
        description = "Establishes a Server-Sent Events (SSE) connection to stream real-time notification bundle IDs to the authenticated user.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully established SSE connection for notification bundle IDs.",
                content = [Content(
                    mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                    schema = Schema(implementation = BundleIdDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication is required and has failed or has not yet been provided."
            )
        ]
    )
    fun sse(): Flux<ServerSentEvent<BundleIdDto>>? =
        notificationService.sendBundleIdsFlux()

    @GetMapping("/unseen")
    @Operation(
        operationId = "getUnseenNotificationsCountForUser",
        tags = ["Notifications"],
        summary = "Get Unseen Notifications Count",
        description = "Retrieves the count of unseen notifications for the authenticated user.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved unseen notifications count.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = Long::class)
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication is required and has failed or has not yet been provided."
            )
        ]

    )
    fun getUnseenNotificationsCountForUser(): Long =
        notificationService.getUnseenNotificationsCount()

    @GetMapping
    @Operation(
        operationId = "getNotificationsForUser",
        tags = ["Notifications"],
        summary = "Get Notification Bundles for User",
        description = "Retrieves notification bundles for the authenticated user. Returns both unseen notification bundles (not yet marked as seen) and paginated seen bundles.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved notification bundles.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = NotificationBundleListDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication is required and has failed or has not yet been provided."
            )
        ]
    )
    fun getNotificationsForUser(
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): NotificationBundleListDto =
        notificationService.getNotificationsForUser(offset, limit)
}