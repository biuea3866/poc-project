package com.biuea.wiki.domain.document

import com.biuea.wiki.domain.document.entity.Document
import com.biuea.wiki.domain.document.entity.DocumentRevision
import com.biuea.wiki.infrastructure.document.DocumentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
) {
    // ── 생성 ──────────────────────────────────────────────────────────────────
    @Transactional
    fun saveDocument(command: SaveDocumentCommand): Pair<Document, DocumentRevision> {
        val parent = command.parentId?.let {
            documentRepository.findByIdAndStatusNot(it)
                ?: throw IllegalArgumentException("부모 문서를 찾을 수 없습니다. id=$it")
        }
        val document = Document(
            title = command.title,
            content = command.content,
            parent = parent,
            createdBy = command.createdBy,
            updatedBy = command.createdBy,
        )
        val revision = DocumentRevision.create(document)
        document.addRevision(revision)
        document.validate()

        val saved = documentRepository.save(document)
        return Pair(saved, saved.latestRevision()!!)
    }

    // ── 수정 ──────────────────────────────────────────────────────────────────
    @Transactional
    fun updateDocument(command: UpdateDocumentCommand): Pair<Document, DocumentRevision> {
        val document = documentRepository.findByIdAndStatusNot(command.documentId)
            ?: throw IllegalArgumentException("문서를 찾을 수 없습니다. id=${command.documentId}")

        check(document.createdBy == command.userId) { "문서 수정 권한이 없습니다." }
        check(!document.isDeleted()) { "삭제된 문서는 수정할 수 없습니다." }

        document.update(command.title, command.content, command.userId)
        val revision = DocumentRevision.create(document)
        document.addRevision(revision)

        val saved = documentRepository.save(document)
        return Pair(saved, saved.latestRevision()!!)
    }

    // ── 발행 ──────────────────────────────────────────────────────────────────
    @Transactional
    fun publishDocument(command: PublishDocumentCommand): Pair<Document, DocumentRevision> {
        val document = documentRepository.findByIdAndStatusNot(command.documentId)
            ?: throw IllegalArgumentException("문서를 찾을 수 없습니다. id=${command.documentId}")

        check(document.createdBy == command.userId) { "문서 발행 권한이 없습니다." }

        document.publish()
        val revision = DocumentRevision.create(document)
        document.addRevision(revision)

        val saved = documentRepository.save(document)
        return Pair(saved, saved.latestRevision()!!)
    }

    // ── 삭제 ──────────────────────────────────────────────────────────────────
    @Transactional
    fun deleteDocument(command: DeleteDocumentCommand) {
        val document = documentRepository.findByIdAndStatusNot(command.documentId)
            ?: throw IllegalArgumentException("문서를 찾을 수 없습니다. id=${command.documentId}")

        check(document.createdBy == command.userId) { "문서 삭제 권한이 없습니다." }

        val allDeleted = document.deleteWithDescendants()
        documentRepository.saveAll(allDeleted)
    }

    // ── 복구 ──────────────────────────────────────────────────────────────────
    @Transactional
    fun restoreDocument(command: RestoreDocumentCommand): Pair<Document, Boolean> {
        val document = documentRepository.findById(command.documentId).orElseThrow {
            IllegalArgumentException("문서를 찾을 수 없습니다. id=${command.documentId}")
        }

        check(document.isDeleted()) { "삭제된 문서만 복구할 수 있습니다." }
        check(document.createdBy == command.userId) { "문서 복구 권한이 없습니다." }

        val hadParent = document.parent != null
        val activeParent = document.resolveActiveParent()
        document.restore(activeParent)

        val saved = documentRepository.save(document)
        // 부모가 있었으나 삭제되어 루트로 복구된 경우 true
        return Pair(saved, hadParent && activeParent == null)
    }
}

