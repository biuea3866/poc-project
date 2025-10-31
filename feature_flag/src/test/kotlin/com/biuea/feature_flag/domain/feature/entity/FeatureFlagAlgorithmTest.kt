package com.biuea.feature_flag.domain.feature.entity

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow

class FeatureFlagAlgorithmTest : DescribeSpec({
    describe("FeatureFlagAlgorithm 인터페이스") {
        describe("FeatureFlagAlgorithmDecider") {
            context("SPECIFIC 알고리즘 옵션으로 결정할 때") {
                it("specifics가 null이면 예외가 발생한다") {
                    // when & then
                    shouldThrow<IllegalArgumentException> {
                        FeatureFlagAlgorithmDecider.decide(
                            algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                            specifics = null,
                            percentage = null,
                            absolute = null
                        )
                    }
                }
                
                it("specifics가 비어있으면 예외가 발생한다") {
                    // when & then
                    shouldThrow<IllegalArgumentException> {
                        FeatureFlagAlgorithmDecider.decide(
                            algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                            specifics = emptyList(),
                            percentage = null,
                            absolute = null
                        )
                    }
                }
                
                it("올바른 specifics가 주어지면 SpecificAlgorithm을 반환한다") {
                    // given
                    val specifics = listOf(1, 2, 3)
                    
                    // when
                    val algorithm = FeatureFlagAlgorithmDecider.decide(
                        algorithmOption = FeatureFlagAlgorithmOption.SPECIFIC,
                        specifics = specifics,
                        percentage = null,
                        absolute = null
                    )
                    
                    // then
                    algorithm shouldBe SpecificAlgorithm(specifics)
                }
            }
            
            context("PERCENT 알고리즘 옵션으로 결정할 때") {
                it("percentage가 null이면 예외가 발생한다") {
                    // when & then
                    shouldThrow<IllegalArgumentException> {
                        FeatureFlagAlgorithmDecider.decide(
                            algorithmOption = FeatureFlagAlgorithmOption.PERCENT,
                            specifics = null,
                            percentage = null,
                            absolute = null
                        )
                    }
                }
                
                it("올바른 percentage가 주어지면 PercentAlgorithm을 반환한다") {
                    // given
                    val percentage = 50
                    
                    // when
                    val algorithm = FeatureFlagAlgorithmDecider.decide(
                        algorithmOption = FeatureFlagAlgorithmOption.PERCENT,
                        specifics = null,
                        percentage = percentage,
                        absolute = null
                    )
                    
                    // then
                    algorithm shouldBe PercentAlgorithm(percentage)
                }
            }
            
            context("ABSOLUTE 알고리즘 옵션으로 결정할 때") {
                it("absolute가 null이면 예외가 발생한다") {
                    // when & then
                    shouldThrow<IllegalArgumentException> {
                        FeatureFlagAlgorithmDecider.decide(
                            algorithmOption = FeatureFlagAlgorithmOption.ABSOLUTE,
                            specifics = null,
                            percentage = null,
                            absolute = null
                        )
                    }
                }
                
                it("올바른 absolute가 주어지면 AbsoluteAlgorithm을 반환한다") {
                    // given
                    val absolute = 100
                    
                    // when
                    val algorithm = FeatureFlagAlgorithmDecider.decide(
                        algorithmOption = FeatureFlagAlgorithmOption.ABSOLUTE,
                        specifics = null,
                        percentage = null,
                        absolute = absolute
                    )
                    
                    // then
                    algorithm shouldBe AbsoluteAlgorithm(absolute)
                }
            }
        }
        
        describe("SpecificAlgorithm") {
            context("isEnabled 메서드 호출 시") {
                it("specifics 크기가 MAX_SPECIFIC_SIZE를 초과하면 예외가 발생한다") {
                    // given
                    val specifics = List(FeatureFlagGroup.MAX_SPECIFIC_SIZE + 1) { it }
                    val algorithm = SpecificAlgorithm(specifics)
                    
                    // when & then
                    shouldThrow<IllegalArgumentException> {
                        algorithm.isEnabled(1)
                    }
                }
                
                it("workspaceId가 specifics에 포함되면 true를 반환한다") {
                    // given
                    val specifics = listOf(1, 2, 3)
                    val algorithm = SpecificAlgorithm(specifics)
                    
                    // when & then
                    algorithm.isEnabled(2) shouldBe true
                }
                
                it("workspaceId가 specifics에 포함되지 않으면 false를 반환한다") {
                    // given
                    val specifics = listOf(1, 2, 3)
                    val algorithm = SpecificAlgorithm(specifics)
                    
                    // when & then
                    algorithm.isEnabled(4) shouldBe false
                }
            }
        }
        
        describe("PercentAlgorithm") {
            context("isEnabled 메서드 호출 시") {
                it("workspaceId를 100으로 나눈 나머지가 percentage 미만이면 true를 반환한다") {
                    // given
                    val percentage = 50
                    val algorithm = PercentAlgorithm(percentage)
                    
                    // when & then
                    algorithm.isEnabled(49) shouldBe true  // 49 % 100 = 49, 49 < 50
                    algorithm.isEnabled(149) shouldBe true // 149 % 100 = 49, 49 < 50
                    algorithm.isEnabled(50) shouldBe false // 50 % 100 = 50, 50 !< 50
                }
                
                it("workspaceId를 100으로 나눈 나머지가 percentage 이상이면 false를 반환한다") {
                    // given
                    val percentage = 50
                    val algorithm = PercentAlgorithm(percentage)
                    
                    // when & then
                    algorithm.isEnabled(50) shouldBe false
                    algorithm.isEnabled(151) shouldBe false // 151 % 100 = 51, 51 >= 50
                }
            }
        }
        
        describe("AbsoluteAlgorithm") {
            context("isEnabled 메서드 호출 시") {
                it("workspaceId가 absolute 이하면 true를 반환한다") {
                    // given
                    val absolute = 100
                    val algorithm = AbsoluteAlgorithm(absolute)
                    
                    // when & then
                    algorithm.isEnabled(100) shouldBe true
                    algorithm.isEnabled(99) shouldBe true
                    algorithm.isEnabled(1) shouldBe true
                }
                
                it("workspaceId가 absolute 초과면 false를 반환한다") {
                    // given
                    val absolute = 100
                    val algorithm = AbsoluteAlgorithm(absolute)
                    
                    // when & then
                    algorithm.isEnabled(101) shouldBe false
                    algorithm.isEnabled(200) shouldBe false
                }
            }
        }
    }
})