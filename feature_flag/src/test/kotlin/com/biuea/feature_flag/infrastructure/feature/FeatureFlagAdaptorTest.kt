package com.biuea.feature_flag.infrastructure.feature

import com.biuea.feature_flag.domain.feature.entity.Feature
import com.biuea.feature_flag.domain.feature.entity.FeatureFlag
import com.biuea.feature_flag.domain.feature.entity.FeatureFlagAlgorithmOption
import com.biuea.feature_flag.domain.feature.entity.FeatureFlagGroup
import com.biuea.feature_flag.domain.feature.entity.FeatureFlagStatus
import com.biuea.feature_flag.infrastructure.feature.jpa.FeatureFlagEntity
import com.biuea.feature_flag.infrastructure.feature.jpa.FeatureFlagGroupEntity
import com.biuea.feature_flag.infrastructure.feature.jpa.FeatureFlagGroupJpaRepository
import com.biuea.feature_flag.infrastructure.feature.jpa.FeatureFlagJpaRepository
import com.biuea.feature_flag.infrastructure.feature.jpa.toEntity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull
import java.time.ZonedDateTime

class FeatureFlagAdaptorTest : DescribeSpec({
    describe("FeatureFlagAdaptor") {
        val featureFlagJpaRepository = mockk<FeatureFlagJpaRepository>()
        val featureFlagGroupJpaRepository = mockk<FeatureFlagGroupJpaRepository>()
        val adaptor = FeatureFlagAdaptor(featureFlagJpaRepository, featureFlagGroupJpaRepository)

        context("FeatureFlagRepository") {
            it("save stores and returns domain FeatureFlag") {
                // given
                val domain = FeatureFlag(
                    _id = 1L,
                    _feature = Feature.AI_SCREENING,
                    _updatedAt = ZonedDateTime.now(),
                    _createdAt = ZonedDateTime.now()
                )
                val savedEntity = domain.toEntity().apply { id = 1L }
                every { featureFlagJpaRepository.save(any()) } returns savedEntity

                // when
                val result = adaptor.save(domain)

                // then
                result.feature shouldBe domain.feature
                verify { featureFlagJpaRepository.save(any()) }
            }

            it("getFeatureFlags returns all mapped FeatureFlags") {
                // given
                val e1 = FeatureFlagEntity(
                    feature = Feature.AI_SCREENING,
                    createdAt = ZonedDateTime.now(),
                    updatedAt = ZonedDateTime.now(),
                ).apply { id = 1L }
                val e2 = FeatureFlagEntity(
                    feature = Feature.APPLICANT_EVALUATOR,
                    createdAt = ZonedDateTime.now(),
                    updatedAt = ZonedDateTime.now(),
                ).apply { id = 2L }
                every { featureFlagJpaRepository.findAll() } returns listOf(e1, e2)

                // when
                val result = adaptor.getFeatureFlags()

                // then
                result.shouldHaveSize(2)
                result[0].id shouldBe e1.id
                result[0].feature shouldBe e1.feature
                result[1].id shouldBe e2.id
                result[1].feature shouldBe e2.feature
            }

            it("getFeatureFlagBy returns mapped domain or throws") {
                // given
                val feature = Feature.AI_SCREENING
                val entity = FeatureFlagEntity(
                    feature = feature,
                    createdAt = ZonedDateTime.now(),
                    updatedAt = ZonedDateTime.now(),
                ).apply { id = 1L }
                every { featureFlagJpaRepository.findByFeatureIs(feature) } returns entity

                // when
                val result = adaptor.getFeatureFlagBy(feature)

                // then
                result.id shouldBe entity.id
                result.feature shouldBe feature

                // and: not found
                every { featureFlagJpaRepository.findByFeatureIs(feature) } returns null
                shouldThrow<NoSuchElementException> { adaptor.getFeatureFlagBy(feature) }
            }

            it("getFeatureFlagOrNullBy returns mapped domain or null") {
                val feature = Feature.AI_SCREENING
                val entity = FeatureFlagEntity(
                    feature = feature,
                    createdAt = ZonedDateTime.now(),
                    updatedAt = ZonedDateTime.now(),
                ).apply { id = 1L }
                every { featureFlagJpaRepository.findByFeatureIs(feature) } returns entity
                adaptor.getFeatureFlagOrNullBy(feature)?.id shouldBe entity.id
                adaptor.getFeatureFlagOrNullBy(feature)?.feature shouldBe feature

                every { featureFlagJpaRepository.findByFeatureIs(feature) } returns null
                adaptor.getFeatureFlagOrNullBy(feature) shouldBe null
            }
        }

        context("FeatureFlagGroupRepository") {
            fun newFeatureFlag(): FeatureFlag {
                return FeatureFlag(
                    _id = 10L,
                    _feature = Feature.AI_SCREENING,
                    _updatedAt = ZonedDateTime.now(),
                    _createdAt = ZonedDateTime.now()
                )
            }

            it("save group stores and returns mapped domain") {
                // given
                val featureFlag = newFeatureFlag()
                val group = FeatureFlagGroup.create(
                    featureFlag = featureFlag,
                    status = FeatureFlagStatus.ACTIVE,
                    algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                    specifics = listOf(1, 2, 3),
                    absolute = null,
                    percentage = null,
                )

                val groupEntity = group.toEntity()
                val ffEntity = featureFlag.toEntity()
                every { featureFlagGroupJpaRepository.save(any()) } returns groupEntity
                every { featureFlagJpaRepository.findByIdOrNull(groupEntity.featureFlagId) } returns ffEntity

                // when
                val result = adaptor.save(group)

                // then
                result.id shouldBe group.id
                result.specifics shouldBe group.specifics
                verify { featureFlagGroupJpaRepository.save(any()) }
            }

            it("getFeatureFlagGroupOrNullBy(id) returns mapped domain or null") {
                val groupId = 1L
                val ffId = 2L
                val ffEntity = FeatureFlagEntity(
                    feature = Feature.AI_SCREENING,
                    createdAt = ZonedDateTime.now(),
                    updatedAt = ZonedDateTime.now(),
                ).apply { id = ffId }
                val groupEntity = FeatureFlagGroupEntity(
                    featureFlagId = ffId,
                    status = FeatureFlagStatus.ACTIVE,
                    specifics = listOf(1, 2, 3),
                    percentage = null,
                    absolute = null,
                    algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                    createdAt = ZonedDateTime.now(),
                    updatedAt = ZonedDateTime.now(),
                ).apply { id = groupId }

                every { featureFlagGroupJpaRepository.findByIdOrNull(groupId) } returns groupEntity
                every { featureFlagJpaRepository.findByIdOrNull(ffId) } returns ffEntity
                adaptor.getFeatureFlagGroupOrNullBy(groupId)?.id shouldBe groupId
                adaptor.getFeatureFlagGroupOrNullBy(groupId)?.featureFlag?.id shouldBe ffId

                every { featureFlagGroupJpaRepository.findByIdOrNull(groupId) } returns null
                adaptor.getFeatureFlagGroupOrNullBy(groupId) shouldBe null

                every { featureFlagGroupJpaRepository.findByIdOrNull(groupId) } returns groupEntity
                every { featureFlagJpaRepository.findByIdOrNull(ffId) } returns null
                adaptor.getFeatureFlagGroupOrNullBy(groupId) shouldBe null
            }

            it("getFeatureFlagGroupOrNullBy(feature) returns mapped domain or null") {
                val feature = Feature.AI_SCREENING
                val ffId = 5L
                val groupId = 9L
                val ffEntity = FeatureFlagEntity(
                    feature = feature,
                    createdAt = ZonedDateTime.now(),
                    updatedAt = ZonedDateTime.now(),
                ).apply { id = ffId }
                val groupEntity = FeatureFlagGroupEntity(
                    featureFlagId = ffId,
                    status = FeatureFlagStatus.ACTIVE,
                    specifics = listOf(1, 2, 3),
                    percentage = null,
                    absolute = null,
                    algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                    createdAt = ZonedDateTime.now(),
                    updatedAt = ZonedDateTime.now(),
                ).apply { id = groupId }

                every { featureFlagJpaRepository.findByFeatureIs(feature) } returns ffEntity
                every { featureFlagGroupJpaRepository.findByFeatureFlagId(ffId) } returns groupEntity
                adaptor.getFeatureFlagGroupOrNullBy(feature)?.id shouldBe groupId
                adaptor.getFeatureFlagGroupOrNullBy(feature)?.featureFlag?.id shouldBe ffId
                adaptor.getFeatureFlagGroupOrNullBy(feature)?.featureFlag?.feature shouldBe feature

                every { featureFlagJpaRepository.findByFeatureIs(feature) } returns null
                adaptor.getFeatureFlagGroupOrNullBy(feature) shouldBe null

                every { featureFlagJpaRepository.findByFeatureIs(feature) } returns ffEntity
                every { featureFlagGroupJpaRepository.findByFeatureFlagId(ffId) } returns null
                adaptor.getFeatureFlagGroupOrNullBy(feature) shouldBe null
            }

            it("getFeatureFlagGroups joins flags and groups and maps all valid ones") {
                val ffId1 = 1L
                val ffId2 = 2L
                val gId1 = 3L
                val gId2 = 4L

                val ffEntity1 = FeatureFlagEntity(
                    feature = Feature.AI_SCREENING,
                    createdAt = ZonedDateTime.now(),
                    updatedAt = ZonedDateTime.now(),
                ).apply { id = ffId1 }
                val ffEntity2 = FeatureFlagEntity(
                    feature = Feature.APPLICANT_EVALUATOR,
                    createdAt = ZonedDateTime.now(),
                    updatedAt = ZonedDateTime.now(),
                ).apply { id = ffId2 }

                val gEntity1 = FeatureFlagGroupEntity(
                    featureFlagId = ffId1,
                    status = FeatureFlagStatus.ACTIVE,
                    specifics = listOf(1, 2, 3),
                    percentage = null,
                    absolute = null,
                    algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                    createdAt = ZonedDateTime.now(),
                    updatedAt = ZonedDateTime.now(),
                ).apply { id = gId1 }
                val gEntity2 = FeatureFlagGroupEntity(
                    featureFlagId = ffId2,
                    status = FeatureFlagStatus.ACTIVE,
                    specifics = emptyList(),
                    percentage = 50,
                    absolute = null,
                    algorithmOption = FeatureFlagAlgorithmOption.PERCENT,
                    createdAt = ZonedDateTime.now(),
                    updatedAt = ZonedDateTime.now(),
                ).apply { id = gId2 }

                every { featureFlagJpaRepository.findAll() } returns listOf(ffEntity1, ffEntity2)
                every { featureFlagGroupJpaRepository.findAll() } returns listOf(gEntity1, gEntity2)

                val result = adaptor.getFeatureFlagGroups()

                result.shouldHaveSize(2)
                result[0].id shouldBe gId1
                result[0].featureFlag.id shouldBe ffId1
                result[1].id shouldBe gId2
                result[1].featureFlag.id shouldBe ffId2
            }

            it("getFeatureFlagGroups filters out groups without matching feature flag") {
                val ffId1 = 1L
                val gId1 = 3L
                val gId2 = 4L

                val ffEntity1 = FeatureFlagEntity(
                    feature = Feature.AI_SCREENING,
                    createdAt = ZonedDateTime.now(),
                    updatedAt = ZonedDateTime.now(),
                ).apply { id = ffId1 }

                val gEntity1 = FeatureFlagGroupEntity(
                    featureFlagId = ffId1,
                    status = FeatureFlagStatus.ACTIVE,
                    specifics = listOf(1, 2, 3),
                    percentage = null,
                    absolute = null,
                    algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                    createdAt = ZonedDateTime.now(),
                    updatedAt = ZonedDateTime.now(),
                ).apply { id = gId1 }
                val gEntity2 = FeatureFlagGroupEntity(
                    featureFlagId = 999L,
                    status = FeatureFlagStatus.ACTIVE,
                    specifics = emptyList(),
                    percentage = 50,
                    absolute = null,
                    algorithmOption = FeatureFlagAlgorithmOption.PERCENT,
                    createdAt = ZonedDateTime.now(),
                    updatedAt = ZonedDateTime.now(),
                ).apply { id = gId2 }

                every { featureFlagJpaRepository.findAll() } returns listOf(ffEntity1)
                every { featureFlagGroupJpaRepository.findAll() } returns listOf(gEntity1, gEntity2)

                val result = adaptor.getFeatureFlagGroups()

                result.shouldHaveSize(1)
                result[0].id shouldBe gId1
                result[0].featureFlag.id shouldBe ffId1
            }

            it("delete group delegates to JPA repository") {
                val featureFlag = newFeatureFlag()
                val group = FeatureFlagGroup.create(
                    featureFlag = featureFlag,
                    status = FeatureFlagStatus.INACTIVE,
                    algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                    specifics = listOf(1),
                    absolute = null,
                    percentage = null,
                )

                every { featureFlagGroupJpaRepository.delete(any()) } returns Unit

                adaptor.delete(group)

                verify { featureFlagGroupJpaRepository.delete(any()) }
            }
        }
    }
})
