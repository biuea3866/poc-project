package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.person.EmergencyContact

data class UpdateEmergencyContactsCommand(
    val viewerEmploymentId: Long,
    val personId: Long,
    val contacts: List<EmergencyContact>,
)
