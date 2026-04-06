package com.closet.common.response

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorResponse? = null,
) {
    companion object {
        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)

        fun <T> created(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)

        fun fail(error: ErrorResponse): ApiResponse<Nothing> = ApiResponse(success = false, error = error)
    }
}

data class ErrorResponse(
    val code: String,
    val message: String,
    val details: List<String> = emptyList(),
)
