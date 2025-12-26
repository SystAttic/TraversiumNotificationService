package traversium.notification.exceptions

import com.google.firebase.auth.FirebaseAuthException
import jakarta.validation.ConstraintViolationException
import org.apache.logging.log4j.kotlin.Logging
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
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

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        logger.warn(e) { "Invalid parameter type: ${e.name}" }
        val message = "Invalid value '${e.value}' for parameter '${e.name}'. Expected type: ${e.requiredType?.simpleName}"
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Invalid parameter type",
                    message = message
                )
            )
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> {
        logger.warn(e) { "Missing required parameter: ${e.parameterName}" }
        val message = "Required parameter '${e.parameterName}' of type '${e.parameterType}' is missing"
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Missing required parameter",
                    message = message
                )
            )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(e: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        logger.warn(e) { "Constraint violation: ${e.message}" }
        val violations = e.constraintViolations.joinToString(", ") { "${it.propertyPath}: ${it.message}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Validation failed",
                    message = violations
                )
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        logger.warn(e) { "Validation failed: ${e.message}" }
        val errors = e.bindingResult.allErrors.joinToString(", ") {
            val fieldName = (it as? FieldError)?.field ?: "unknown"
            "$fieldName: ${it.defaultMessage}"
        }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Validation failed",
                    message = errors
                )
            )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        logger.warn(e) { "Malformed JSON request: ${e.message}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "Malformed request",
                    message = "Request body is not readable or contains invalid JSON"
                )
            )
    }

    @ExceptionHandler(DataAccessException::class)
    fun handleDataAccessException(e: DataAccessException): ResponseEntity<ErrorResponse> {
        logger.error(e) { "Database error: ${e.message}" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    error = "Database error",
                    message = "An error occurred while accessing the database"
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