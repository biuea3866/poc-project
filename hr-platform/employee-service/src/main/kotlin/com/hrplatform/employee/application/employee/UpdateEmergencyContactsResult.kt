package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.person.EmergencyContact
import com.hrplatform.employee.domain.person.Person

data class UpdateEmergencyContactsResult(
    val personId: Long,
    val contacts: List<EmergencyContact>,
) {
    companion object {
        fun of(person: Person): UpdateEmergencyContactsResult = UpdateEmergencyContactsResult(
            personId = requireNotNull(person.id) { "저장된 Person에는 id가 있어야 합니다" },
            contacts = person.emergencyContacts,
        )
    }
}
