package com.hrplatform.auth.presentation.controller

import com.hrplatform.core.exception.BusinessException
import com.hrplatform.core.exception.ForbiddenException
import com.hrplatform.core.exception.NotFoundException
import com.hrplatform.core.exception.UnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ApiError(
    val code: String,
    val message: String,
)

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(exception: UnauthorizedException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiError(code = exception.errorCode, message = exception.message ?: "인증이 필요합니다"))

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(exception: ForbiddenException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiError(code = exception.errorCode, message = exception.message ?: "접근 권한이 없습니다"))

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(exception: NotFoundException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiError(code = exception.errorCode, message = exception.message ?: "리소스를 찾을 수 없습니다"))

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(exception: BusinessException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiError(code = exception.errorCode, message = exception.message ?: "요청을 처리할 수 없습니다"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val message = exception.bindingResult.fieldErrors
            .firstOrNull()?.defaultMessage ?: "요청 값이 올바르지 않습니다"
        return ResponseEntity.badRequest()
            .body(ApiError(code = "VALIDATION_ERROR", message = message))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(exception: IllegalArgumentException): ResponseEntity<ApiError> =
        ResponseEntity.badRequest()
            .body(ApiError(code = "BAD_REQUEST", message = exception.message ?: "잘못된 요청입니다"))

    @ExceptionHandler(Exception::class)
    fun handleGeneral(exception: Exception): ResponseEntity<ApiError> {
        log.error("예상치 못한 오류 발생", exception)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError(code = "INTERNAL_ERROR", message = "서버 내부 오류가 발생했습니다"))
    }
}
