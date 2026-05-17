package com.hrplatform.employee.application.employee

data class UpdatePersonalInfoCommand(
    val viewerEmploymentId: Long,
    val personId: Long,
    val personalEmail: String,
    val phoneNumber: String?,
)
