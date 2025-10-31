package com.biuea.feature_flag.domain.feature.entity

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import java.time.ZonedDateTime

class FeatureFlagGroupTest : DescribeSpec({
    describe("FeatureFlagGroup 엔티티") {
        val featureFlag = FeatureFlag(
            _id = 1L,
            _feature = Feature.AI_SCREENING,
            _updatedAt = ZonedDateTime.now(),
            _createdAt = ZonedDateTime.now()
        )
        
        context("create 메서드로 생성할 때") {
            context("SPECIFIC 알고리즘 옵션으로 생성 시") {
                it("specifics가 비어있으면 예외가 발생한다") {
                    // when & then
                    shouldThrow<IllegalArgumentException> {
                        FeatureFlagGroup.create(
                            featureFlag = featureFlag,
                            status = FeatureFlagStatus.ACTIVE,
                            algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                            specifics = emptyList(),
                            absolute = null,
                            percentage = null
                        )
                    }
                }
                
                it("specifics 크기가 MAX_SPECIFIC_SIZE를 초과하면 예외가 발생한다") {
                    // given
                    val specifics = List(FeatureFlagGroup.MAX_SPECIFIC_SIZE + 1) { it }
                    
                    // when & then
                    shouldThrow<IllegalArgumentException> {
                        FeatureFlagGroup.create(
                            featureFlag = featureFlag,
                            status = FeatureFlagStatus.ACTIVE,
                            algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                            specifics = specifics,
                            absolute = null,
                            percentage = null
                        )
                    }
                }
                
                it("올바른 specifics가 주어지면 FeatureFlagGroup이 생성된다") {
                    // given
                    val specifics = listOf(1, 2, 3)
                    
                    // when
                    val featureFlagGroup = FeatureFlagGroup.create(
                        featureFlag = featureFlag,
                        status = FeatureFlagStatus.ACTIVE,
                        algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                        specifics = specifics,
                        absolute = null,
                        percentage = null
                    )
                    
                    // then
                    featureFlagGroup.featureFlag shouldBe featureFlag
                    featureFlagGroup.specifics shouldBe specifics
                    featureFlagGroup.absolute shouldBe null
                    featureFlagGroup.percentage shouldBe null
                }
            }
            
            context("PERCENT 알고리즘 옵션으로 생성 시") {
                it("percentage가 null이면 예외가 발생한다") {
                    // when & then
                    shouldThrow<IllegalArgumentException> {
                        FeatureFlagGroup.create(
                            featureFlag = featureFlag,
                            status = FeatureFlagStatus.ACTIVE,
                            algorithmOption = FeatureFlagAlgorithmOption.PERCENT,
                            specifics = emptyList(),
                            absolute = null,
                            percentage = null
                        )
                    }
                }
                
                it("올바른 percentage가 주어지면 FeatureFlagGroup이 생성된다") {
                    // given
                    val percentage = 50
                    
                    // when
                    val featureFlagGroup = FeatureFlagGroup.create(
                        featureFlag = featureFlag,
                        status = FeatureFlagStatus.ACTIVE,
                        algorithmOption = FeatureFlagAlgorithmOption.PERCENT,
                        specifics = emptyList(),
                        absolute = null,
                        percentage = percentage
                    )
                    
                    // then
                    featureFlagGroup.featureFlag shouldBe featureFlag
                    featureFlagGroup.specifics shouldBe emptyList()
                    featureFlagGroup.absolute shouldBe null
                    featureFlagGroup.percentage shouldBe percentage
                }
            }
            
            context("ABSOLUTE 알고리즘 옵션으로 생성 시") {
                it("absolute가 null이면 예외가 발생한다") {
                    // when & then
                    shouldThrow<IllegalArgumentException> {
                        FeatureFlagGroup.create(
                            featureFlag = featureFlag,
                            status = FeatureFlagStatus.ACTIVE,
                            algorithmOption = FeatureFlagAlgorithmOption.ABSOLUTE,
                            specifics = emptyList(),
                            absolute = null,
                            percentage = null
                        )
                    }
                }
                
                it("올바른 absolute가 주어지면 FeatureFlagGroup이 생성된다") {
                    // given
                    val absolute = 100
                    
                    // when
                    val featureFlagGroup = FeatureFlagGroup.create(
                        featureFlag = featureFlag,
                        status = FeatureFlagStatus.ACTIVE,
                        algorithmOption = FeatureFlagAlgorithmOption.ABSOLUTE,
                        specifics = emptyList(),
                        absolute = absolute,
                        percentage = null
                    )
                    
                    // then
                    featureFlagGroup.featureFlag shouldBe featureFlag
                    featureFlagGroup.specifics shouldBe emptyList()
                    featureFlagGroup.absolute shouldBe absolute
                    featureFlagGroup.percentage shouldBe null
                }
            }
        }
        
        context("containsWorkspace 메서드 호출 시") {
            it("알고리즘이 초기화되지 않았으면 예외가 발생한다") {
                // given
                val featureFlagGroup = FeatureFlagGroup(
                    _id = 1L,
                    _featureFlag = featureFlag,
                    _status = FeatureFlagStatus.ACTIVE,
                    _specifics = emptyList(),
                    _percentage = null,
                    _absolute = null,
                    _updatedAt = ZonedDateTime.now(),
                    _createdAt = ZonedDateTime.now()
                )
                
                // when & then
                shouldThrow<IllegalStateException> {
                    featureFlagGroup.containsWorkspace(1)
                }
            }
            
            it("알고리즘이 초기화되었으면 알고리즘의 isEnabled 결과를 반환한다") {
                // given
                val specifics = listOf(1, 2, 3)
                val featureFlagGroup = FeatureFlagGroup.create(
                    featureFlag = featureFlag,
                    status = FeatureFlagStatus.ACTIVE,
                    algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                    specifics = specifics,
                    absolute = null,
                    percentage = null
                )
                
                // when & then
                featureFlagGroup.containsWorkspace(2) shouldBe true
                featureFlagGroup.containsWorkspace(4) shouldBe false
            }
        }
        
        context("상태 변경 및 검증") {
            it("activate/inactivate 동작을 확인한다") {
                // given
                val group = FeatureFlagGroup.create(
                    featureFlag = featureFlag,
                    status = FeatureFlagStatus.INACTIVE,
                    algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                    specifics = listOf(1),
                    absolute = null,
                    percentage = null
                )
                
                // when
                group.activate()
                
                // then
                group.isActivation() shouldBe true
                
                // when
                group.inactivate()
                
                // then
                group.isActivation() shouldBe false
            }
            
            it("비활성 상태면 checkActivation에서 예외가 발생한다") {
                // given
                val group = FeatureFlagGroup.create(
                    featureFlag = featureFlag,
                    status = FeatureFlagStatus.INACTIVE,
                    algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                    specifics = listOf(1),
                    absolute = null,
                    percentage = null
                )
                
                // when & then
                shouldThrow<IllegalStateException> { group.checkActivation() }
            }
        }
    }
})