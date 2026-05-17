package com.hrplatform.core.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.ZonedDateTime

class BaseEntityTest : BehaviorSpec({

    given("저장된 Entity에 softDelete를 호출할 때") {
        val now = ZonedDateTime.now()
        val entity = TestBaseEntity(id = 1L)

        `when`("softDelete(now, by) 를 호출하면") {
            entity.softDelete(now, 42L)

            then("deletedAt이 채워진다") {
                entity.deletedAt shouldBe now
            }

            then("deletedBy가 채워진다") {
                entity.deletedBy shouldBe 42L
            }

            then("isDeleted가 true다") {
                entity.isDeleted shouldBe true
            }
        }
    }

    given("이미 삭제된 Entity에 softDelete를 재호출할 때") {
        val now = ZonedDateTime.now()
        val entity = TestBaseEntity(id = 2L)
        entity.softDelete(now, 1L)

        `when`("softDelete를 두 번째 호출하면") {
            then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    entity.softDelete(now.plusSeconds(1), 2L)
                }
            }
        }
    }

    given("삭제된 Entity에 restore를 호출할 때") {
        val now = ZonedDateTime.now()
        val entity = TestBaseEntity(id = 3L)
        entity.softDelete(now, 10L)

        `when`("restore()를 호출하면") {
            entity.restore()

            then("deletedAt이 null로 복원된다") {
                entity.deletedAt shouldBe null
            }

            then("deletedBy가 null로 복원된다") {
                entity.deletedBy shouldBe null
            }

            then("isDeleted가 false다") {
                entity.isDeleted shouldBe false
            }
        }
    }

    given("삭제되지 않은 Entity에 restore를 호출할 때") {
        val entity = TestBaseEntity(id = 4L)

        `when`("restore()를 호출하면") {
            then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    entity.restore()
                }
            }
        }
    }

    given("equals/hashCode 검증") {
        `when`("같은 클래스 + 같은 id면") {
            val entityA = TestBaseEntity(id = 10L)
            val entityB = TestBaseEntity(id = 10L)

            then("equals는 true다") {
                entityA shouldBe entityB
            }
        }

        `when`("같은 클래스지만 다른 id면") {
            val entityA = TestBaseEntity(id = 10L)
            val entityB = TestBaseEntity(id = 20L)

            then("equals는 false다") {
                entityA shouldNotBe entityB
            }
        }

        `when`("다른 클래스면") {
            val entity = TestBaseEntity(id = 10L)
            val other = AnotherBaseEntity(id = 10L)

            then("equals는 false다") {
                (entity == other) shouldBe false
            }
        }

        `when`("id가 null이면") {
            val entityA = TestBaseEntity(id = null)
            val entityB = TestBaseEntity(id = null)

            then("equals는 false다 (identity 비교)") {
                (entityA == entityB) shouldBe false
            }
        }
    }
})

private class TestBaseEntity(id: Long?) : BaseEntity() {
    init {
        this.id = id
    }
}

private class AnotherBaseEntity(id: Long?) : BaseEntity() {
    init {
        this.id = id
    }
}
