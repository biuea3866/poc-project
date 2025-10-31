package com.biuea.feature_flag.domain.feature.service

import com.biuea.feature_flag.domain.feature.entity.Feature
import com.biuea.feature_flag.domain.feature.entity.FeatureFlag
import com.biuea.feature_flag.domain.feature.entity.FeatureFlagAlgorithmOption
import com.biuea.feature_flag.domain.feature.entity.FeatureFlagGroup
import com.biuea.feature_flag.domain.feature.entity.FeatureFlagStatus
import com.biuea.feature_flag.domain.feature.repository.FeatureFlagGroupRepository
import com.biuea.feature_flag.domain.feature.repository.FeatureFlagRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime

class FeatureFlagServiceTest : DescribeSpec({
    describe("FeatureFlagService") {
        val featureFlagRepository = mockk<FeatureFlagRepository>()
        val featureFlagGroupRepository = mockk<FeatureFlagGroupRepository>()
        val service = FeatureFlagService(featureFlagRepository, featureFlagGroupRepository)

        context("registerFeatureFlag") {
            it("이미 존재하면 예외") {
                // given
                every { featureFlagRepository.getFeatureFlagOrNullBy(Feature.AI_SCREENING) } returns FeatureFlag(
                    _id = 1L,
                    _feature = Feature.AI_SCREENING,
                    _updatedAt = ZonedDateTime.now(),
                    _createdAt = ZonedDateTime.now()
                )

                // expect
                shouldThrow<IllegalStateException> { service.registerFeatureFlag(Feature.AI_SCREENING) }
            }

            it("존재하지 않으면 저장") {
                // given
                val saved = FeatureFlag(
                    _id = 10L,
                    _feature = Feature.AI_SCREENING,
                    _updatedAt = ZonedDateTime.now(),
                    _createdAt = ZonedDateTime.now()
                )
                every { featureFlagRepository.getFeatureFlagOrNullBy(Feature.AI_SCREENING) } returns null
                every { featureFlagRepository.save(any()) } returns saved

                // when
                val result = service.registerFeatureFlag(Feature.AI_SCREENING)

                // then
                result shouldBe saved
                verify { featureFlagRepository.save(any()) }
            }
        }

        context("activateFeatureFlag (Group)") {
            it("없는 그룹이면 예외") {
                every { featureFlagGroupRepository.getFeatureFlagGroupOrNullBy(1L) } returns null
                shouldThrow<NoSuchElementException> { service.activateFeatureFlag(1L, FeatureFlagStatus.ACTIVE) }
            }

            it("ACTIVE 요청이면 그룹 활성화 후 저장") {
                val group = mockk<FeatureFlagGroup>(relaxed = true)
                every { featureFlagGroupRepository.getFeatureFlagGroupOrNullBy(1L) } returns group
                every { featureFlagGroupRepository.save(group) } returns group

                service.activateFeatureFlag(1L, FeatureFlagStatus.ACTIVE)

                verify { group.activate() }
                verify { featureFlagGroupRepository.save(group) }
            }

            it("INACTIVE 요청이면 그룹 비활성화 후 저장") {
                val group = mockk<FeatureFlagGroup>(relaxed = true)
                every { featureFlagGroupRepository.getFeatureFlagGroupOrNullBy(1L) } returns group
                every { featureFlagGroupRepository.save(group) } returns group

                service.activateFeatureFlag(1L, FeatureFlagStatus.INACTIVE)

                verify { group.inactivate() }
                verify { featureFlagGroupRepository.save(group) }
            }
        }

        context("fetchFeatureFlagGroups") {
            it("워크스페이스에 활성화된 그룹만 반환한다") {
                val workspaceId = 7
                val ff = FeatureFlag(
                    _id = 1L,
                    _feature = Feature.AI_SCREENING,
                    _updatedAt = ZonedDateTime.now(),
                    _createdAt = ZonedDateTime.now()
                )
                val g1 = FeatureFlagGroup.create(
                    featureFlag = ff,
                    status = FeatureFlagStatus.ACTIVE,
                    algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                    specifics = listOf(workspaceId),
                    absolute = null,
                    percentage = null
                )
                val g2 = FeatureFlagGroup.create(
                    featureFlag = ff,
                    status = FeatureFlagStatus.INACTIVE,
                    algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                    specifics = listOf(workspaceId),
                    absolute = null,
                    percentage = null
                )
                val g3 = FeatureFlagGroup.create(
                    featureFlag = ff,
                    status = FeatureFlagStatus.ACTIVE,
                    algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                    specifics = listOf(999),
                    absolute = null,
                    percentage = null
                )
                every { featureFlagGroupRepository.getFeatureFlagGroups() } returns listOf(g1, g2, g3)

                val result = service.fetchFeatureFlagGroups(workspaceId)

                result.shouldContainExactly(g1)
            }
        }

        context("decideFeatureFlagGroup") {
            it("이미 그룹 존재하면 예외") {
                val ff = FeatureFlag(
                    _id = 1L,
                    _feature = Feature.AI_SCREENING,
                    _updatedAt = ZonedDateTime.now(),
                    _createdAt = ZonedDateTime.now()
                )
                val existing = FeatureFlagGroup.create(
                    featureFlag = ff,
                    status = FeatureFlagStatus.ACTIVE,
                    algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                    specifics = listOf(1),
                    absolute = null,
                    percentage = null
                )
                every { featureFlagGroupRepository.getFeatureFlagGroupOrNullBy(Feature.AI_SCREENING) } returns existing

                shouldThrow<IllegalStateException> {
                    service.decideFeatureFlagGroup(
                        feature = Feature.AI_SCREENING,
                        status = FeatureFlagStatus.ACTIVE,
                        algorithm = FeatureFlagAlgorithmOption.SPECIFIC,
                        specifics = listOf(1),
                        percentage = null,
                        absolute = null,
                    )
                }
            }

            it("새 그룹 생성 및 저장") {
                val ff = FeatureFlag(
                    _id = 9L,
                    _feature = Feature.AI_SCREENING,
                    _updatedAt = ZonedDateTime.now(),
                    _createdAt = ZonedDateTime.now()
                )
                every { featureFlagGroupRepository.getFeatureFlagGroupOrNullBy(Feature.AI_SCREENING) } returns null
                every { featureFlagRepository.getFeatureFlagBy(Feature.AI_SCREENING) } returns ff
                every { featureFlagGroupRepository.save(any()) } answers { firstArg() }

                service.decideFeatureFlagGroup(
                    feature = Feature.AI_SCREENING,
                    status = FeatureFlagStatus.ACTIVE,
                    algorithm = FeatureFlagAlgorithmOption.ABSOLUTE,
                    specifics = emptyList(),
                    percentage = null,
                    absolute = 100,
                )

                verify { featureFlagGroupRepository.save(any()) }
            }
        }

        context("deleteFeatureFlagGroup") {
            it("없는 그룹이면 예외") {
                every { featureFlagGroupRepository.getFeatureFlagGroupOrNullBy(123L) } returns null
                shouldThrow<NoSuchElementException> { service.deleteFeatureFlagGroup(123L) }
            }

            it("존재하면 삭제 호출") {
                val ff = FeatureFlag(
                    _id = 1L,
                    _feature = Feature.AI_SCREENING,
                    _updatedAt = ZonedDateTime.now(),
                    _createdAt = ZonedDateTime.now()
                )
                val group = FeatureFlagGroup.create(
                    featureFlag = ff,
                    status = FeatureFlagStatus.ACTIVE,
                    algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                    specifics = listOf(1),
                    absolute = null,
                    percentage = null
                )
                every { featureFlagGroupRepository.getFeatureFlagGroupOrNullBy(5L) } returns group
                every { featureFlagGroupRepository.delete(group) } returns Unit

                service.deleteFeatureFlagGroup(5L)

                verify { featureFlagGroupRepository.delete(group) }
            }
        }
    }
})
