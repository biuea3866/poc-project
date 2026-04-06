package com.closet.display.domain.entity

import com.closet.common.exception.BusinessException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MagazineTest : BehaviorSpec({

    Given("매거진 생성") {
        When("매거진을 생성하면") {
            val magazine =
                Magazine(
                    title = "2026 SS 트렌드 가이드",
                    subtitle = "올 봄 꼭 알아야 할 패션 키워드",
                    contentBody = "이번 시즌 주목해야 할 트렌드를 소개합니다...",
                    thumbnailUrl = "https://cdn.closet.com/magazine/ss2026.jpg",
                    category = "TREND",
                    authorName = "패션에디터",
                )

            Then("미발행 상태로 생성된다") {
                magazine.title shouldBe "2026 SS 트렌드 가이드"
                magazine.isPublished shouldBe false
                magazine.publishedAt shouldBe null
                magazine.viewCount shouldBe 0
            }
        }
    }

    Given("매거진 발행") {
        val magazine =
            Magazine(
                title = "봄 코디 매거진",
                contentBody = "봄 코디 추천...",
                category = "STYLING",
                authorName = "스타일리스트",
            )

        When("발행하면") {
            magazine.publish()

            Then("발행 상태가 된다") {
                magazine.isPublished shouldBe true
                magazine.publishedAt shouldNotBe null
            }
        }

        When("이미 발행된 매거진을 다시 발행 시도") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    magazine.publish()
                }
            }
        }
    }

    Given("매거진 비발행") {
        val magazine =
            Magazine(
                title = "취소할 매거진",
                contentBody = "내용",
                category = "NEWS",
                authorName = "에디터",
            )
        magazine.publish()

        When("비발행하면") {
            magazine.unpublish()

            Then("비발행 상태가 된다") {
                magazine.isPublished shouldBe false
            }
        }

        When("비발행 상태에서 다시 비발행 시도") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    magazine.unpublish()
                }
            }
        }
    }

    Given("조회수 증가") {
        val magazine =
            Magazine(
                title = "인기 매거진",
                contentBody = "인기 콘텐츠",
                category = "TREND",
                authorName = "에디터",
            )

        When("조회수를 증가시키면") {
            magazine.incrementViewCount()
            magazine.incrementViewCount()

            Then("조회수가 증가한다") {
                magazine.viewCount shouldBe 2
            }
        }
    }

    Given("매거진 수정") {
        val magazine =
            Magazine(
                title = "원래 제목",
                contentBody = "원래 내용",
                category = "TREND",
                authorName = "에디터",
            )

        When("수정하면") {
            magazine.update(
                title = "수정된 제목",
                subtitle = "추가된 부제목",
                contentBody = "수정된 내용",
                thumbnailUrl = "https://cdn.closet.com/new.jpg",
                category = "STYLING",
                authorName = "새 에디터",
            )

            Then("정보가 변경된다") {
                magazine.title shouldBe "수정된 제목"
                magazine.subtitle shouldBe "추가된 부제목"
                magazine.contentBody shouldBe "수정된 내용"
                magazine.category shouldBe "STYLING"
                magazine.authorName shouldBe "새 에디터"
            }
        }
    }

    Given("매거진 상품 관리") {
        val magazine =
            Magazine(
                title = "상품 연결 매거진",
                contentBody = "상품 소개",
                category = "LOOKBOOK",
                authorName = "에디터",
            )

        When("상품을 추가하면") {
            val product = MagazineProduct(productId = 100L, sortOrder = 1)
            magazine.addProduct(product)

            Then("상품이 추가된다") {
                magazine.products.size shouldBe 1
                magazine.products[0].productId shouldBe 100L
            }
        }

        When("상품을 제거하면") {
            magazine.removeProduct(100L)

            Then("상품이 제거된다") {
                magazine.products.size shouldBe 0
            }
        }

        When("존재하지 않는 상품 제거 시도") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    magazine.removeProduct(999L)
                }
            }
        }
    }

    Given("매거진 태그 관리") {
        val magazine =
            Magazine(
                title = "태그 매거진",
                contentBody = "태그 테스트",
                category = "TREND",
                authorName = "에디터",
            )

        When("태그를 추가하면") {
            val tag = MagazineTag(tagName = "봄코디")
            magazine.addTag(tag)

            Then("태그가 추가된다") {
                magazine.tags.size shouldBe 1
                magazine.tags[0].tagName shouldBe "봄코디"
            }
        }

        When("태그를 제거하면") {
            magazine.removeTag("봄코디")

            Then("태그가 제거된다") {
                magazine.tags.size shouldBe 0
            }
        }

        When("존재하지 않는 태그 제거 시도") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    magazine.removeTag("없는태그")
                }
            }
        }
    }
})
