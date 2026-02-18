package com.biuea.wiki.infrastructure.document

import com.biuea.wiki.domain.document.entity.Document
import com.biuea.wiki.domain.document.entity.DocumentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface DocumentRepository : JpaRepository<Document, Long> {

    // 삭제되지 않은 문서 단건 조회
    fun findByIdAndStatusNot(id: Long, status: DocumentStatus = DocumentStatus.DELETED): Document?

    // 루트 ACTIVE 문서 목록 (사이드바 트리용)
    fun findByParentIsNullAndStatus(status: DocumentStatus, pageable: Pageable): Page<Document>

    // 특정 부모의 ACTIVE 하위 문서 목록
    fun findByParentIdAndStatus(parentId: Long, status: DocumentStatus, pageable: Pageable): Page<Document>

    // 특정 상태의 내 문서 목록 (DRAFT 목록, Trash 조회 등)
    fun findByCreatedByAndStatus(createdBy: Long, status: DocumentStatus, pageable: Pageable): Page<Document>

    // ai_status 업데이트를 위한 ID 기반 조회 (worker에서 사용)
    fun findByIdAndStatus(id: Long, status: DocumentStatus): Document?
}
