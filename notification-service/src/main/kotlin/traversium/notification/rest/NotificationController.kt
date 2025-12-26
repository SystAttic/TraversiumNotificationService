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
import traversium.notification.dto.NotificationBundleDto
import traversium.notification.dto.NotificationBundleListDto
import traversium.notification.exceptions.ExceptionHandler.ErrorResponse
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
                description = "Unauthorized - Authentication is required and has failed or has not yet been provided.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal Server Error - An unexpected error occurred.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            )
        ]
    )
    fun sse(): Flux<ServerSentEvent<BundleIdDto>>? =
        notificationService.sendBundleIdsFlux()

    @GetMapping("/unseen/count")
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
                description = "Unauthorized - Authentication is required and has failed or has not yet been provided.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal Server Error - An unexpected error occurred.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class)
                )]
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
                responseCode = "400",
                description = "Bad Request - Invalid parameter values provided.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication is required and has failed or has not yet been provided.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal Server Error - An unexpected error occurred.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            )
        ]
    )
    fun getNotificationsForUser(
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): NotificationBundleListDto =
        notificationService.getNotificationsForUser(offset, limit)

    @GetMapping("/unseen")
    @Operation(
        operationId = "getUnseenNotificationBundlesForUser",
        tags = ["Notifications"],
        summary = "Get Unseen Notification Bundles",
        description = "Retrieves unseen notification bundles for the authenticated user and marks them as seen. This endpoint works the same way as the previous toggle endpoint - it returns unseen notifications and automatically converts them to seen bundles.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved unseen notification bundles.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = NotificationBundleDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request - Invalid parameter values provided.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication is required and has failed or has not yet been provided.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal Server Error - An unexpected error occurred.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            )
        ]
    )
    fun getUnseenNotificationBundlesForUser(
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): List<NotificationBundleDto> =
        notificationService.getUnseenNotificationsForUser(offset, limit)

    @GetMapping("/seen")
    @Operation(
        operationId = "getSeenNotificationBundlesForUser",
        tags = ["Notifications"],
        summary = "Get Seen Notification Bundles",
        description = "Retrieves seen notification bundles for the authenticated user with pagination support.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved seen notification bundles.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = NotificationBundleDto::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Bad Request - Invalid parameter values provided.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized - Authentication is required and has failed or has not yet been provided.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal Server Error - An unexpected error occurred.",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            )
        ]
    )
    fun getSeenNotificationBundlesForUser(
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "20") limit: Int
    ): List<NotificationBundleDto> =
        notificationService.getSeenNotificationsForUser(offset, limit)
}