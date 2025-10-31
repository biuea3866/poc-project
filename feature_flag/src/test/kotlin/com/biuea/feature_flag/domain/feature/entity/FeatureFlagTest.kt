package com.biuea.feature_flag.domain.feature.entity

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.ZonedDateTime

class FeatureFlagTest : DescribeSpec({
    describe("FeatureFlag 엔티티") {
        context("create 메서드로 생성할 때") {
            it("올바른 상태로 생성된다") {
                // given
                val feature = Feature.AI_SCREENING
                
                // when
                val featureFlag = FeatureFlag.create(feature)
                
                // then
                featureFlag.feature shouldBe feature
                featureFlag.id shouldBe 0
            }
        }
        
        context("기능 매칭 확인") {
            it("isMatchFeature는 동일한 기능일 때 true를 반환한다") {
                // given
                val featureFlag = FeatureFlag(
                    _id = 1L,
                    _feature = Feature.AI_SCREENING,
                    _updatedAt = ZonedDateTime.now(),
                    _createdAt = ZonedDateTime.now()
                )
                
                // expect
                featureFlag.isMatchFeature(Feature.AI_SCREENING) shouldBe true
                featureFlag.isMatchFeature(Feature.APPLICANT_EVALUATOR) shouldBe false
            }
        }
    }
})