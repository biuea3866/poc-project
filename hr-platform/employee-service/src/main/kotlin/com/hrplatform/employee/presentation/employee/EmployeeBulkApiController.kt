package com.hrplatform.employee.presentation.employee

import com.hrplatform.employee.application.employee.BulkHireCommand
import com.hrplatform.employee.application.employee.BulkHireUseCase
import com.hrplatform.employee.application.employee.BulkRecordEmploymentEventsCommand
import com.hrplatform.employee.application.employee.BulkRecordEmploymentEventsUseCase
import com.hrplatform.employee.application.employee.BulkResult
import com.hrplatform.employee.presentation.auth.AuthEmploymentId
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employees/bulk")
class EmployeeBulkApiController(
    private val bulkHireUseCase: BulkHireUseCase,
    private val bulkRecordEventsUseCase: BulkRecordEmploymentEventsUseCase,
) {

    @PostMapping
    fun hire(
        @RequestBody @Valid requests: List<HireRequest>,
        @AuthEmploymentId actorId: Long?,
    ): BulkResult {
        val command = BulkHireCommand(commands = requests.map { it.toCommand(actorId) })
        return bulkHireUseCase.execute(command)
    }

    @PostMapping("/employment-events")
    fun events(
        @RequestBody @Valid requests: List<BulkRecordEventRequest>,
        @AuthEmploymentId actorId: Long?,
    ): BulkResult {
        val command = BulkRecordEmploymentEventsCommand(
            commands = requests.map { it.toCommand(actorEmploymentId = actorId) },
        )
        return bulkRecordEventsUseCase.execute(command)
    }
}
