package com.biuea.aiwiki.application.document

import com.biuea.aiwiki.domain.document.model.AiStatus
import com.biuea.aiwiki.domain.document.model.Document
import com.biuea.aiwiki.domain.document.model.DocumentRevision
import com.biuea.aiwiki.domain.document.model.DocumentStatus
import com.biuea.aiwiki.domain.document.port.DocumentRepository
import com.biuea.aiwiki.domain.document.port.DocumentRevisionRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.Instant

class DocumentCommandServiceTest : DescribeSpec({
    describe("DocumentCommandService") {
        it("초안 생성 시 DRAFT와 NOT_STARTED 상태로 저장한다") {
            val documentRepository = FakeDocumentRepository()
            val revisionRepository = FakeDocumentRevisionRepository()
            val service = DocumentCommandService(documentRepository, revisionRepository)

            val result = service.createDraft(
                CreateDraftCommand(
                    title = "새 문서",
                    content = "본문",
                    tags = listOf("ai"),
                    userId = 1L,
                )
            )

            result.status shouldBe DocumentStatus.DRAFT
            result.aiStatus shouldBe AiStatus.NOT_STARTED
            revisionRepository.saved shouldHaveSize 0
        }

        it("ACTIVE 전환 시 revision을 남긴다") {
            val documentRepository = FakeDocumentRepository(
                seed = mutableMapOf(
                    1L to Document(
                        id = 1L,
                        title = "문서",
                        content = "본문",
                        createdBy = 1L,
                        updatedBy = 1L,
                    )
                )
            )
            val revisionRepository = FakeDocumentRevisionRepository()
            val service = DocumentCommandService(documentRepository, revisionRepository)

            val result = service.activate(1L, 1L)

            result.status shouldBe DocumentStatus.ACTIVE
            revisionRepository.saved shouldHaveSize 1
            revisionRepository.saved.first().status shouldBe DocumentStatus.ACTIVE
        }

        it("ACTIVE 문서 수정 시 revision을 남긴다") {
            val documentRepository = FakeDocumentRepository(
                seed = mutableMapOf(
                    1L to Document(
                        id = 1L,
                        title = "문서",
                        content = "본문",
                        status = DocumentStatus.ACTIVE,
                        createdBy = 1L,
                        updatedBy = 1L,
                        updatedAt = Instant.parse("2026-03-14T00:00:00Z"),
                    )
                )
            )
            val revisionRepository = FakeDocumentRevisionRepository()
            val service = DocumentCommandService(documentRepository, revisionRepository)

            val result = service.updateDraft(
                UpdateDraftCommand(
                    documentId = 1L,
                    title = "수정 문서",
                    content = "수정 본문",
                    tags = listOf("ai", "wiki"),
                    userId = 1L,
                    expectedUpdatedAt = Instant.parse("2026-03-14T00:00:00Z"),
                )
            )

            result.title shouldBe "수정 문서"
            revisionRepository.saved shouldHaveSize 1
            revisionRepository.saved.first().title shouldBe "수정 문서"
        }

        it("updated_at이 다르면 낙관적 잠금 예외가 발생한다") {
            val documentRepository = FakeDocumentRepository(
                seed = mutableMapOf(
                    1L to Document(
                        id = 1L,
                        title = "문서",
                        content = "본문",
                        status = DocumentStatus.DRAFT,
                        createdBy = 1L,
                        updatedBy = 1L,
                        updatedAt = Instant.parse("2026-03-14T00:00:00Z"),
                    )
                )
            )
            val revisionRepository = FakeDocumentRevisionRepository()
            val service = DocumentCommandService(documentRepository, revisionRepository)

            shouldThrow<OptimisticLockException> {
                service.updateDraft(
                    UpdateDraftCommand(
                        documentId = 1L,
                        title = "수정 문서",
                        content = "수정 본문",
                        tags = listOf("ai"),
                        userId = 1L,
                        expectedUpdatedAt = Instant.parse("2026-03-14T00:00:01Z"),
                    )
                )
            }.message shouldBe "문서가 이미 변경되었습니다. 최신 상태를 다시 조회하세요."
        }
    }
})

private class FakeDocumentRepository(
    private val seed: MutableMap<Long, Document> = mutableMapOf(),
) : DocumentRepository {
    private var nextId: Long = if (seed.isEmpty()) 1L else seed.keys.max() + 1

    override fun save(document: Document): Document {
        val persisted = if (document.id == null) {
            Document(
                id = nextId++,
                title = document.title,
                content = document.content,
                status = document.status,
                aiStatus = document.aiStatus,
                tags = document.tags,
                createdBy = document.createdBy,
                updatedBy = document.updatedBy,
                createdAt = document.createdAt,
                updatedAt = Instant.parse("2026-03-14T00:00:00Z"),
            )
        } else {
            document.markUpdated(Instant.parse("2026-03-14T00:00:10Z"))
            document
        }
        seed[requireNotNull(persisted.id)] = persisted
        return persisted
    }

    override fun findById(id: Long): Document? = seed[id]

    override fun searchActiveOwnedByUser(query: String, userId: Long): List<Document> =
        seed.values.filter { it.createdBy == userId && it.status == DocumentStatus.ACTIVE }
}

private class FakeDocumentRevisionRepository : DocumentRevisionRepository {
    val saved: MutableList<DocumentRevision> = mutableListOf()

    override fun save(revision: DocumentRevision): DocumentRevision {
        saved.add(revision)
        return revision
    }

    override fun findByDocumentId(documentId: Long): List<DocumentRevision> =
        saved.filter { it.documentId == documentId }
}
