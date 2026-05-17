package com.hrplatform.employee.application.employee

import com.hrplatform.core.exception.ForbiddenException
import com.hrplatform.employee.domain.employment.EmploymentDomainService
import com.hrplatform.employee.domain.person.PersonDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdatePersonalInfoUseCase(
    private val personDomainService: PersonDomainService,
    private val employmentDomainService: EmploymentDomainService,
) {
    @Transactional
    fun execute(command: UpdatePersonalInfoCommand): UpdatePersonalInfoResult {
        val viewerEmployment = employmentDomainService.getById(command.viewerEmploymentId)
        if (viewerEmployment.personId != command.personId) {
            throw ForbiddenException("ACCESS_DENIED", "본인 정보만 수정할 수 있습니다")
        }
        val person = personDomainService.updateContact(
            personId = command.personId,
            personalEmail = command.personalEmail,
            phoneNumber = command.phoneNumber,
        )
        return UpdatePersonalInfoResult.of(person)
    }
}
