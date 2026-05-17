package com.hrplatform.employee.application.employee

data class BulkHireCommand(
    val commands: List<HireEmployeeCommand>,
)
