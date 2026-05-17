package com.hrplatform.employee.presentation.employee

import com.hrplatform.employee.application.employee.CancelEmploymentEventUseCase
import com.hrplatform.employee.application.employee.GetEmployeeCommand
import com.hrplatform.employee.application.employee.GetEmployeeUseCase
import com.hrplatform.employee.application.employee.GetEmploymentHistoryCommand
import com.hrplatform.employee.application.employee.GetEmploymentHistoryUseCase
import com.hrplatform.employee.application.employee.HireEmployeeResult
import com.hrplatform.employee.application.employee.HireEmployeeUseCase
import com.hrplatform.employee.application.employee.RecordEmploymentEventUseCase
import com.hrplatform.employee.application.employee.ResignEmploymentUseCase
import com.hrplatform.employee.application.employee.ResumeEmploymentCommand
import com.hrplatform.employee.application.employee.ResumeEmploymentUseCase
import com.hrplatform.employee.application.employee.SearchEmployeesCommand
import com.hrplatform.employee.application.employee.SearchEmployeesUseCase
import com.hrplatform.employee.application.employee.SuspendEmploymentUseCase
import com.hrplatform.employee.presentation.auth.AuthEmploymentId
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employees")
class EmployeeApiController(
    private val hireUseCase: HireEmployeeUseCase,
    private val resignUseCase: ResignEmploymentUseCase,
    private val recordEventUseCase: RecordEmploymentEventUseCase,
    private val cancelEventUseCase: CancelEmploymentEventUseCase,
    private val suspendUseCase: SuspendEmploymentUseCase,
    private val resumeUseCase: ResumeEmploymentUseCase,
    private val searchUseCase: SearchEmployeesUseCase,
    private val getUseCase: GetEmployeeUseCase,
    private val getHistoryUseCase: GetEmploymentHistoryUseCase,
) {

    @PostMapping
    fun hire(
        @RequestBody @Valid request: HireRequest,
        @AuthEmploymentId actorId: Long?,
    ): ResponseEntity<HireEmployeeResult> {
        val result = hireUseCase.execute(request.toCommand(actorId))
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @GetMapping
    fun search(
        @AuthEmploymentId viewerEmploymentId: Long,
        @RequestParam companyId: Long,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) departmentId: Long?,
        @PageableDefault(size = 20) pageable: Pageable,
    ) = searchUseCase.execute(
        SearchEmployeesCommand(
            viewerEmploymentId = viewerEmploymentId,
            companyId = companyId,
            keyword = keyword,
            departmentId = departmentId,
            pageable = pageable,
        ),
    )

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: Long,
        @AuthEmploymentId viewerEmploymentId: Long,
    ) = getUseCase.execute(GetEmployeeCommand(viewerEmploymentId = viewerEmploymentId, targetEmploymentId = id))

    @PostMapping("/{id}/employment-events")
    fun recordEvent(
        @PathVariable id: Long,
        @RequestBody @Valid request: RecordEventRequest,
        @AuthEmploymentId actorId: Long?,
    ) = recordEventUseCase.execute(request.toCommand(employmentId = id, actorEmploymentId = actorId))

    @DeleteMapping("/{id}/employment-events/{eventId}")
    fun cancelEvent(
        @PathVariable id: Long,
        @PathVariable eventId: Long,
        @RequestBody @Valid request: CancelEventRequest,
        @AuthEmploymentId actorId: Long?,
    ) = cancelEventUseCase.execute(request.toCommand(employmentId = id, historyId = eventId, actorEmploymentId = actorId))

    @PostMapping("/{id}/suspend")
    fun suspend(
        @PathVariable id: Long,
        @RequestBody @Valid request: SuspendRequest,
        @AuthEmploymentId actorId: Long?,
    ) = suspendUseCase.execute(request.toCommand(employmentId = id, actorEmploymentId = actorId))

    @PostMapping("/{id}/resume")
    fun resume(
        @PathVariable id: Long,
        @AuthEmploymentId actorId: Long?,
    ) = resumeUseCase.execute(ResumeEmploymentCommand(employmentId = id, actorEmploymentId = actorId))

    @PostMapping("/{id}/resign")
    fun resign(
        @PathVariable id: Long,
        @RequestBody @Valid request: ResignRequest,
        @AuthEmploymentId actorId: Long?,
    ) = resignUseCase.execute(request.toCommand(employmentId = id, actorEmploymentId = actorId))

    @GetMapping("/{id}/employment-events")
    fun getHistory(
        @PathVariable id: Long,
        @AuthEmploymentId viewerEmploymentId: Long,
    ) = getHistoryUseCase.execute(GetEmploymentHistoryCommand(viewerEmploymentId = viewerEmploymentId, employmentId = id))
}
