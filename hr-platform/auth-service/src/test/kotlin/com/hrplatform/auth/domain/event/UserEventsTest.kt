package com.hrplatform.auth.domain.event

import com.hrplatform.auth.domain.account.event.UserCreatedEvent
import com.hrplatform.auth.domain.account.event.UserDeactivatedEvent
import com.hrplatform.auth.domain.account.event.UserLockedEvent
import com.hrplatform.auth.domain.account.event.UserPasswordChangedEvent
import com.hrplatform.auth.domain.account.event.UserReactivatedEvent
import com.hrplatform.auth.domain.account.event.UserRoleAssignedEvent
import com.hrplatform.auth.domain.account.event.UserRoleRevokedEvent
import com.hrplatform.auth.domain.account.event.UserSuspendedEvent
import com.hrplatform.auth.domain.account.event.UserTwoFactorDisabledEvent
import com.hrplatform.auth.domain.account.event.UserTwoFactorEnrolledEvent
import com.hrplatform.auth.domain.account.event.UserUnlockedEvent
import com.hrplatform.core.domain.DomainEventEnvelope
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.ZonedDateTime

class UserEventsTest : BehaviorSpec({

    val now = ZonedDateTime.now()
    val userAccountId = 1L
    val companyId = 10L
    val employmentId = 100L
    val email = "user@example.com"

    given("UserCreatedEvent") {
        `when`("envelope 변환") {
            val event = UserCreatedEvent(
                userAccountId = userAccountId,
                companyIdValue = companyId,
                employmentId = employmentId,
                email = email,
                defaultRoleCode = "EMPLOYEE",
                actorEmploymentId = null,
                occurredAt = now,
            )
            val envelope = DomainEventEnvelope.from(event)

            then("eventType=UserCreated, aggregateType=UserAccount") {
                envelope.eventType shouldBe "UserCreated"
                envelope.aggregateType shouldBe "UserAccount"
                envelope.eventVersion shouldBe 1
            }
            then("action.type=CREATE, details에 employmentId/email/defaultRole 포함") {
                envelope.action.type shouldBe "CREATE"
                (envelope.action.details.containsKey("employmentId")) shouldBe true
                (envelope.action.details.containsKey("email")) shouldBe true
                (envelope.action.details.containsKey("defaultRole")) shouldBe true
            }
            then("state.status=ACTIVE, snapshot에 4개 필드 포함") {
                envelope.state.status shouldBe "ACTIVE"
                (envelope.state.snapshot.containsKey("employmentId")) shouldBe true
                (envelope.state.snapshot.containsKey("email")) shouldBe true
                (envelope.state.snapshot.containsKey("twoFactorEnabled")) shouldBe true
                (envelope.state.snapshot.containsKey("lockedUntil")) shouldBe true
            }
        }
    }

    given("UserLockedEvent") {
        val event = UserLockedEvent(
            userAccountId = userAccountId,
            companyIdValue = companyId,
            employmentId = employmentId,
            email = email,
            failedAttempts = 5,
            lockedUntil = now.plusMinutes(15),
            twoFactorEnabled = false,
            actorEmploymentId = null,
            occurredAt = now,
        )
        val envelope = DomainEventEnvelope.from(event)

        then("action.type=LOCK, state.status=LOCKED") {
            envelope.action.type shouldBe "LOCK"
            envelope.state.status shouldBe "LOCKED"
            envelope.action.details["failedAttempts"] shouldBe 5
            envelope.action.details["lockedUntil"] shouldNotBe null
        }
    }

    given("UserUnlockedEvent") {
        `when`("AUTO trigger") {
            val event = UserUnlockedEvent(
                userAccountId = userAccountId,
                companyIdValue = companyId,
                employmentId = employmentId,
                email = email,
                twoFactorEnabled = false,
                trigger = "AUTO",
                actorEmploymentId = null,
                occurredAt = now,
            )
            val envelope = DomainEventEnvelope.from(event)

            then("action.type=UNLOCK, details.trigger=AUTO") {
                envelope.action.type shouldBe "UNLOCK"
                envelope.action.details["trigger"] shouldBe "AUTO"
                envelope.state.status shouldBe "ACTIVE"
            }
        }
    }

    given("UserSuspendedEvent") {
        val event = UserSuspendedEvent(
            userAccountId = userAccountId,
            companyIdValue = companyId,
            employmentId = employmentId,
            email = email,
            twoFactorEnabled = false,
            reason = "휴직",
            actorEmploymentId = 99L,
            occurredAt = now,
        )
        val envelope = DomainEventEnvelope.from(event)

        then("action.type=SUSPEND, state.status=SUSPENDED") {
            envelope.action.type shouldBe "SUSPEND"
            envelope.state.status shouldBe "SUSPENDED"
            envelope.action.details["reason"] shouldBe "휴직"
        }
    }

    given("UserReactivatedEvent") {
        val event = UserReactivatedEvent(
            userAccountId = userAccountId,
            companyIdValue = companyId,
            employmentId = employmentId,
            email = email,
            twoFactorEnabled = false,
            actorEmploymentId = null,
            occurredAt = now,
        )
        val envelope = DomainEventEnvelope.from(event)

        then("action.type=REACTIVATE, state.status=ACTIVE") {
            envelope.action.type shouldBe "REACTIVATE"
            envelope.state.status shouldBe "ACTIVE"
        }
    }

    given("UserDeactivatedEvent") {
        val event = UserDeactivatedEvent(
            userAccountId = userAccountId,
            companyIdValue = companyId,
            employmentId = employmentId,
            email = email,
            twoFactorEnabled = false,
            reason = "퇴사",
            actorEmploymentId = null,
            occurredAt = now,
        )
        val envelope = DomainEventEnvelope.from(event)

        then("action.type=DEACTIVATE, state.status=DEACTIVATED") {
            envelope.action.type shouldBe "DEACTIVATE"
            envelope.state.status shouldBe "DEACTIVATED"
            envelope.action.details["reason"] shouldBe "퇴사"
        }
    }

    given("UserRoleAssignedEvent") {
        val event = UserRoleAssignedEvent(
            userAccountId = userAccountId,
            companyIdValue = companyId,
            employmentId = employmentId,
            email = email,
            twoFactorEnabled = false,
            lockedUntil = null,
            roleId = 2L,
            roleCode = "TEAM_LEAD",
            actorEmploymentId = 99L,
            occurredAt = now,
        )
        val envelope = DomainEventEnvelope.from(event)

        then("action.type=ASSIGN_ROLE, details에 roleId/roleCode/actor 포함") {
            envelope.action.type shouldBe "ASSIGN_ROLE"
            envelope.action.details["roleId"] shouldBe 2L
            envelope.action.details["roleCode"] shouldBe "TEAM_LEAD"
        }
    }

    given("UserRoleRevokedEvent") {
        val event = UserRoleRevokedEvent(
            userAccountId = userAccountId,
            companyIdValue = companyId,
            employmentId = employmentId,
            email = email,
            twoFactorEnabled = false,
            lockedUntil = null,
            roleId = 2L,
            roleCode = "TEAM_LEAD",
            actorEmploymentId = 99L,
            occurredAt = now,
        )
        val envelope = DomainEventEnvelope.from(event)

        then("action.type=REVOKE_ROLE") {
            envelope.action.type shouldBe "REVOKE_ROLE"
        }
    }

    given("UserPasswordChangedEvent") {
        val event = UserPasswordChangedEvent(
            userAccountId = userAccountId,
            companyIdValue = companyId,
            employmentId = employmentId,
            email = email,
            twoFactorEnabled = false,
            trigger = "SELF_CHANGE",
            actorEmploymentId = null,
            occurredAt = now,
        )
        val envelope = DomainEventEnvelope.from(event)

        then("action.type=CHANGE_PASSWORD, details.trigger=SELF_CHANGE") {
            envelope.action.type shouldBe "CHANGE_PASSWORD"
            envelope.action.details["trigger"] shouldBe "SELF_CHANGE"
        }
    }

    given("UserTwoFactorEnrolledEvent") {
        val event = UserTwoFactorEnrolledEvent(
            userAccountId = userAccountId,
            companyIdValue = companyId,
            employmentId = employmentId,
            email = email,
            actorEmploymentId = null,
            occurredAt = now,
        )
        val envelope = DomainEventEnvelope.from(event)

        then("action.type=ENROLL_2FA, details.method=TOTP, state.snapshot.twoFactorEnabled=true") {
            envelope.action.type shouldBe "ENROLL_2FA"
            envelope.action.details["method"] shouldBe "TOTP"
            envelope.state.snapshot["twoFactorEnabled"] shouldBe true
        }
    }

    given("UserTwoFactorDisabledEvent") {
        val event = UserTwoFactorDisabledEvent(
            userAccountId = userAccountId,
            companyIdValue = companyId,
            employmentId = employmentId,
            email = email,
            actorEmploymentId = null,
            occurredAt = now,
        )
        val envelope = DomainEventEnvelope.from(event)

        then("action.type=DISABLE_2FA, state.snapshot.twoFactorEnabled=false") {
            envelope.action.type shouldBe "DISABLE_2FA"
            envelope.state.snapshot["twoFactorEnabled"] shouldBe false
        }
    }
})
