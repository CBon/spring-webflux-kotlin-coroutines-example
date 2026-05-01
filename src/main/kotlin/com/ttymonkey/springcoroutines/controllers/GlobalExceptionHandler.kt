package com.ttymonkey.springcoroutines.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import kotlin.time.Clock
import kotlin.time.Instant

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String?,
    val path: String?,
    val timestamp: Instant = Clock.System.now(),
    val requestId: String?
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(
        ex: ResponseStatusException,
        request: ServerWebExchange,
    ): ResponseEntity<ErrorResponse> {
        val body = ErrorResponse(
            status = ex.statusCode.value(),
            error = ex.reason ?: ex.message,
            message = ex.reason,
            path = request.request.path.value(),
            requestId = request.request.id
        )
        return ResponseEntity.status(ex.statusCode).body(body)
    }

    // Можно добавить другие обработчики
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: ServerWebExchange,
    ): ResponseEntity<ErrorResponse> {
        val body = ErrorResponse(
            status = 500,
            error = "Internal Server Error",
            message = ex.message,
            path = request.request.path.value(),
            requestId = request.request.id
        )
        return ResponseEntity.internalServerError().body(body)
    }
}