package com.biuea.feature_flag.application.applicant

import com.biuea.feature_flag.domain.applicant.ApplicantService
import com.biuea.feature_flag.domain.feature.entity.Feature
import com.biuea.feature_flag.domain.feature.entity.FeatureFlag
import com.biuea.feature_flag.domain.feature.entity.FeatureFlagAlgorithmOption
import com.biuea.feature_flag.domain.feature.entity.FeatureFlagGroup
import com.biuea.feature_flag.domain.feature.entity.FeatureFlagStatus
import com.biuea.feature_flag.domain.feature.service.FeatureFlagService
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.clearMocks
import java.time.ZonedDateTime

class ApplicantFacadeTest : DescribeSpec({
    describe("ApplicantFacade 클래스") {
        val featureFlagService = mockk<FeatureFlagService>()
        val applicantService = spyk(ApplicantService())
        val applicantFacade = ApplicantFacade(featureFlagService, applicantService)

        fun groupFor(
            feature: Feature,
            workspaceId: Int,
            status: FeatureFlagStatus = FeatureFlagStatus.ACTIVE
        ): FeatureFlagGroup {
            val featureFlag = FeatureFlag(
                _id = 0L,
                _feature = feature,
                _updatedAt = ZonedDateTime.now(),
                _createdAt = ZonedDateTime.now()
            )
            return FeatureFlagGroup.create(
                featureFlag = featureFlag,
                status = status,
                algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                specifics = listOf(workspaceId),
                absolute = null,
                percentage = null
            )
        }

        beforeTest { clearMocks(applicantService) }

        context("execute 메서드 호출 시") {
            it("AI_SCREENING 기능이 활성화되어 있으면 aiScreeningFeature 메서드를 호출한다") {
                // given
                val workspaceId = 1
                val groups = listOf(
                    groupFor(Feature.AI_SCREENING, workspaceId)
                )

                every { featureFlagService.fetchFeatureFlagGroups(workspaceId) } returns groups

                // when
                applicantFacade.execute(workspaceId)

                // then
                verify { applicantService.aiScreeningFeature() }
                verify(exactly = 0) { applicantService.applicantEvaluatorFeature() }
                verify(atLeast = 1) { applicantService.commonBusiness() }
            }

            it("APPLICANT_EVALUATOR 기능이 활성화되어 있으면 applicantEvaluatorFeature 메서드를 호출한다") {
                // given
                val workspaceId = 1
                val groups = listOf(
                    groupFor(Feature.APPLICANT_EVALUATOR, workspaceId)
                )

                every { featureFlagService.fetchFeatureFlagGroups(workspaceId) } returns groups

                // when
                applicantFacade.execute(workspaceId)

                // then
                verify(exactly = 0) { applicantService.aiScreeningFeature() }
                verify { applicantService.applicantEvaluatorFeature() }
                verify(atLeast = 1) { applicantService.commonBusiness() }
            }

            it("활성화된 기능이 없으면 commonBusiness 메서드를 호출한다") {
                // given
                val workspaceId = 1
                val groups = emptyList<FeatureFlagGroup>()

                every { featureFlagService.fetchFeatureFlagGroups(workspaceId) } returns groups

                // when
                applicantFacade.execute(workspaceId)

                // then
                verify(exactly = 0) { applicantService.aiScreeningFeature() }
                verify(exactly = 0) { applicantService.applicantEvaluatorFeature() }
                verify(atLeast = 1) { applicantService.commonBusiness() }
            }

            it("AI_SCREENING과 APPLICANT_EVALUATOR 기능이 모두 활성화되어 있으면 두 기능 모두 실행된다") {
                // given
                val workspaceId = 1
                val groups = listOf(
                    groupFor(Feature.AI_SCREENING, workspaceId),
                    groupFor(Feature.APPLICANT_EVALUATOR, workspaceId)
                )

                every { featureFlagService.fetchFeatureFlagGroups(workspaceId) } returns groups

                // when
                applicantFacade.execute(workspaceId)

                // then
                verify { applicantService.aiScreeningFeature() }
                verify { applicantService.applicantEvaluatorFeature() }
                verify(atLeast = 1) { applicantService.commonBusiness() }
            }
        }
    }
})
