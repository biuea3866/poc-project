package com.closet.common.exception

import com.closet.common.response.ApiResponse
import com.closet.common.response.ErrorResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn { "BusinessException: ${e.errorCode.code} - ${e.message}" }
        val errorResponse = ErrorResponse(
            code = e.errorCode.code,
            message = e.message
        )
        return ResponseEntity
            .status(e.errorCode.status)
            .body(ApiResponse.fail(errorResponse))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn { "Validation failed: ${e.message}" }
        val details = e.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        val errorResponse = ErrorResponse(
            code = ErrorCode.INVALID_INPUT.code,
            message = ErrorCode.INVALID_INPUT.message,
            details = details
        )
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.fail(errorResponse))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn { "IllegalArgumentException: ${e.message}" }
        val errorResponse = ErrorResponse(
            code = ErrorCode.INVALID_INPUT.code,
            message = e.message ?: ErrorCode.INVALID_INPUT.message
        )
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.fail(errorResponse))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error(e) { "Unhandled exception: ${e.message}" }
        val errorResponse = ErrorResponse(
            code = ErrorCode.INTERNAL_SERVER_ERROR.code,
            message = ErrorCode.INTERNAL_SERVER_ERROR.message
        )
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.fail(errorResponse))
    }
}
