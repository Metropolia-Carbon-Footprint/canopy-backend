package fi.metropolia.canopy.exception

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(
        exception: ResourceNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> = buildResponse(
        status = HttpStatus.NOT_FOUND,
        message = exception.message ?: "Resource was not found",
        path = request.requestURI
    )

    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequest(
        exception: InvalidRequestException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> = buildResponse(
        status = HttpStatus.BAD_REQUEST,
        message = exception.message ?: "Request was invalid",
        path = request.requestURI
    )

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(
        exception: ConflictException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> = buildResponse(
        status = HttpStatus.CONFLICT,
        message = exception.message ?: "Request conflicts with an existing resource",
        path = request.requestURI
    )

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(
        exception: UnauthorizedException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> = buildResponse(
        status = HttpStatus.UNAUTHORIZED,
        message = exception.message ?: "Unauthorized",
        path = request.requestURI
    )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val fieldErrors = exception.bindingResult.fieldErrors.associate { error ->
            error.field to (error.defaultMessage ?: "Invalid value")
        }

        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            message = "Request validation failed",
            path = request.requestURI,
            fieldErrors = fieldErrors
        )
    }

    private fun buildResponse(
        status: HttpStatus,
        message: String,
        path: String,
        fieldErrors: Map<String, String>? = null
    ): ResponseEntity<ApiErrorResponse> = ResponseEntity.status(status).body(
        ApiErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = path,
            fieldErrors = fieldErrors
        )
    )
}
