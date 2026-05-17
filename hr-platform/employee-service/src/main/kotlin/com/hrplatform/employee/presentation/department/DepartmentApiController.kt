package com.hrplatform.employee.presentation.department

import com.hrplatform.employee.application.department.AssignDepartmentHeadUseCase
import com.hrplatform.employee.application.department.CreateDepartmentUseCase
import com.hrplatform.employee.application.department.MoveDepartmentUseCase
import com.hrplatform.employee.presentation.auth.AuthEmploymentId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/departments")
class DepartmentApiController(
    private val createUseCase: CreateDepartmentUseCase,
    private val moveUseCase: MoveDepartmentUseCase,
    private val assignHeadUseCase: AssignDepartmentHeadUseCase,
) {

    @PostMapping
    fun create(
        @RequestBody @Valid request: CreateDepartmentRequest,
        @AuthEmploymentId actorId: Long?,
    ): ResponseEntity<*> {
        val result = createUseCase.execute(request.toCommand(actorId))
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody @Valid request: MoveDepartmentRequest,
        @AuthEmploymentId actorId: Long?,
    ) = moveUseCase.execute(request.toCommand(departmentId = id, actorEmploymentId = actorId))

    @PatchMapping("/{id}/head")
    fun assignHead(
        @PathVariable id: Long,
        @RequestBody @Valid request: AssignHeadRequest,
        @AuthEmploymentId actorId: Long?,
    ) = assignHeadUseCase.execute(request.toCommand(departmentId = id, actorEmploymentId = actorId))
}
