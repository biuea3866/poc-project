package com.hrplatform.employee.application.employee

data class BulkResult(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<BulkFailureItem>,
) {
    companion object {
        fun success(count: Int): BulkResult = BulkResult(
            successCount = count,
            failureCount = 0,
            failures = emptyList(),
        )
    }
}

data class BulkFailureItem(
    val index: Int,
    val reason: String,
)
