package com.hrplatform.employee.presentation.employee

import com.hrplatform.employee.application.employee.UpdateEmergencyContactsCommand
import com.hrplatform.employee.domain.person.EmergencyContact
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

data class UpdateEmergencyContactsRequest(
    @field:NotNull
    @field:Valid
    val contacts: List<EmergencyContact>,
) {
    fun toCommand(viewerEmploymentId: Long, personId: Long): UpdateEmergencyContactsCommand =
        UpdateEmergencyContactsCommand(
            viewerEmploymentId = viewerEmploymentId,
            personId = personId,
            contacts = contacts,
        )
}
