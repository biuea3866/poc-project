package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.person.Person

data class UpdatePersonalInfoResult(
    val personId: Long,
    val personalEmail: String,
    val phoneNumber: String?,
) {
    companion object {
        fun of(person: Person): UpdatePersonalInfoResult = UpdatePersonalInfoResult(
            personId = requireNotNull(person.id) { "저장된 Person에는 id가 있어야 합니다" },
            personalEmail = person.personalEmail,
            phoneNumber = person.phoneNumber,
        )
    }
}
