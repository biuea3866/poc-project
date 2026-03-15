package com.biuea.aiwiki.domain.document.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class DocumentTest : DescribeSpec({
    describe("Document") {
        it("새 문서는 DRAFT와 NOT_STARTED로 시작한다") {
            val document = Document(
                title = "AI Wiki 문서",
                content = "초안 본문",
                createdBy = 1L,
                updatedBy = 1L,
            )

            document.status shouldBe DocumentStatus.DRAFT
            document.aiStatus shouldBe AiStatus.NOT_STARTED
        }

        it("ACTIVE 문서만 분석 요청이 가능하다") {
            val document = Document(
                title = "AI Wiki 문서",
                content = "초안 본문",
                createdBy = 1L,
                updatedBy = 1L,
            )

            shouldThrow<IllegalArgumentException> {
                document.requestAnalysis()
            }.message shouldBe "ACTIVE 문서만 분석할 수 있습니다."
        }

        it("PROCESSING 중에는 분석을 다시 요청할 수 없다") {
            val document = Document(
                title = "AI Wiki 문서",
                content = "초안 본문",
                status = DocumentStatus.ACTIVE,
                aiStatus = AiStatus.PROCESSING,
                createdBy = 1L,
                updatedBy = 1L,
            )

            shouldThrow<IllegalArgumentException> {
                document.requestAnalysis()
            }.message shouldBe "PROCESSING 중에는 재요청할 수 없습니다."
        }

        it("ACTIVE 전환 후 분석 요청 시 PENDING으로 바뀐다") {
            val document = Document(
                title = "AI Wiki 문서",
                content = "초안 본문",
                createdBy = 1L,
                updatedBy = 1L,
            )

            document.activate(userId = 1L)
            document.requestAnalysis()

            document.status shouldBe DocumentStatus.ACTIVE
            document.aiStatus shouldBe AiStatus.PENDING
        }

        it("DRAFT 문서를 삭제하면 DELETED로 전환된다") {
            val document = Document(
                title = "삭제 대상",
                content = "본문",
                createdBy = 1L,
                updatedBy = 1L,
            )

            document.delete(userId = 1L)

            document.status shouldBe DocumentStatus.DELETED
        }

        it("ACTIVE 문서를 삭제하면 DELETED로 전환된다") {
            val document = Document(
                title = "삭제 대상",
                content = "본문",
                createdBy = 1L,
                updatedBy = 1L,
            )

            document.activate(userId = 1L)
            document.delete(userId = 1L)

            document.status shouldBe DocumentStatus.DELETED
        }

        it("이미 삭제된 문서는 다시 삭제할 수 없다") {
            val document = Document(
                title = "삭제 대상",
                content = "본문",
                status = DocumentStatus.DELETED,
                createdBy = 1L,
                updatedBy = 1L,
            )

            shouldThrow<IllegalArgumentException> {
                document.delete(userId = 1L)
            }.message shouldBe "이미 삭제된 문서입니다."
        }
    }
})
