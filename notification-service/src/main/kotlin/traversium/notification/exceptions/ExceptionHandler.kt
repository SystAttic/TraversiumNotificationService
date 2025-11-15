package traversium.notification.exceptions

import com.google.firebase.auth.FirebaseAuthException
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

/**
 * Global exception handler for the Notification Service
 * @author Maja Razinger
 */
@RestControllerAdvice
class ExceptionHandler : Logging {

    data class ErrorResponse(
        val timestamp: LocalDateTime = LocalDateTime.now(),
        val status: Int,
        val error: String,
        val message: String?
    )

    @ExceptionHandler(FirebaseAuthException::class)
    fun handleFirebaseAuthException(e: FirebaseAuthException): ResponseEntity<ErrorResponse> {
        logger.warn(e) { "Firebase authentication failed: ${e.message}" }
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                ErrorResponse(
                    status = HttpStatus.UNAUTHORIZED.value(),
                    error = "Authentication failed",
                    message = e.message
                )
            )
    }

    @ExceptionHandler(NotificationExceptions.InvalidNotificationDataException::class)
    fun handleInvalidNotificationDataException(e: NotificationExceptions.InvalidNotificationDataException): ResponseEntity<ErrorResponse> {
        logger.warn(e) { "Invalid notification data: ${e.message}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Invalid notification data",
                    message = e.message
                )
            )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.warn(e) { "Illegal argument: ${e.message}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Invalid request",
                    message = e.message
                )
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ResponseEntity<ErrorResponse> {
        logger.error(e) { "Unexpected error: ${e.message}" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    error = "Internal server error",
                    message = "An unexpected error occurred"
                )
            )
    }
}