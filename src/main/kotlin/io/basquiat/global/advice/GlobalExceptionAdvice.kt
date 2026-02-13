package io.basquiat.global.advice

import io.basquiat.global.exceptions.NatsResponseException
import io.basquiat.global.model.response.ApiError
import io.basquiat.global.utils.logger
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.time.LocalDateTime.now

@RestControllerAdvice
class GlobalExceptionAdvice {
    private val log = logger<GlobalExceptionAdvice>()

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(NatsResponseException::class)
    fun handleNatsResponseException(
        ex: NatsResponseException,
        request: HttpServletRequest,
    ): ApiError {
        log.error("NatsResponseException error: $ex")
        return ApiError(
            path = request.requestURI,
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            message = ex.message!!,
            timestamp = now(),
        )
    }

    /**
     * Inter server error 처리
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception::class)
    fun handleException(
        ex: Exception,
        request: HttpServletRequest,
    ): ApiError {
        if (request.requestURI != "/favicon.ico" && ex !is NoResourceFoundException) {
            log.error("handleException error: $ex")
        }
        return ApiError(
            path = request.requestURI,
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            message = ex.message!!,
            timestamp = now(),
        )
    }
}