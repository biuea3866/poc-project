package com.hrplatform.employee.presentation.employee

import com.hrplatform.employee.application.employee.GetEmployeeCommand
import com.hrplatform.employee.application.employee.GetEmployeeUseCase
import com.hrplatform.employee.application.employee.UpdateEmergencyContactsUseCase
import com.hrplatform.employee.application.employee.UpdatePersonalInfoUseCase
import com.hrplatform.employee.presentation.auth.AuthEmploymentId
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employees/me")
class MyEmployeeApiController(
    private val updatePersonalInfoUseCase: UpdatePersonalInfoUseCase,
    private val updateEmergencyContactsUseCase: UpdateEmergencyContactsUseCase,
    private val getUseCase: GetEmployeeUseCase,
) {

    @GetMapping
    fun me(@AuthEmploymentId actorId: Long) =
        getUseCase.execute(GetEmployeeCommand(viewerEmploymentId = actorId, targetEmploymentId = actorId))

    @PatchMapping
    fun updateInfo(
        @AuthEmploymentId actorId: Long,
        @RequestParam personId: Long,
        @RequestBody @Valid request: UpdatePersonalInfoRequest,
    ) = updatePersonalInfoUseCase.execute(request.toCommand(viewerEmploymentId = actorId, personId = personId))

    @PatchMapping("/emergency-contacts")
    fun updateEmergency(
        @AuthEmploymentId actorId: Long,
        @RequestParam personId: Long,
        @RequestBody @Valid request: UpdateEmergencyContactsRequest,
    ) = updateEmergencyContactsUseCase.execute(request.toCommand(viewerEmploymentId = actorId, personId = personId))
}
