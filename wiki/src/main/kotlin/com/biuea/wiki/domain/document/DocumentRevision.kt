package com.biuea.wiki.domain.document

import com.biuea.wiki.domain.ai.AiAgentLog
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "document_revision")
@EntityListeners(AuditingEntityListener::class)
class DocumentRevision(
    @Column(nullable = false, columnDefinition = "TEXT")
    val data: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    val document: Document,

    @Column(name = "created_by", nullable = false, updatable = false)
    val createdBy: Long,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) {

    @OneToMany(mappedBy = "documentRevision", fetch = FetchType.LAZY)
    val tags: MutableList<DocumentTag> = mutableListOf()

    @OneToMany(mappedBy = "documentRevision", fetch = FetchType.LAZY)
    val summaries: MutableList<DocumentSummary> = mutableListOf()

    @OneToMany(mappedBy = "documentRevision", fetch = FetchType.LAZY)
    val agentLogs: MutableList<AiAgentLog> = mutableListOf()
}
