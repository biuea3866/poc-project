package com.hrplatform.employee.application.employee

data class BulkRecordEmploymentEventsCommand(
    val commands: List<RecordEmploymentEventCommand>,
)
