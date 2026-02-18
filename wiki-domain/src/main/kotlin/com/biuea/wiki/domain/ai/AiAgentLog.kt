package com.biuea.wiki.domain.ai

import com.biuea.wiki.domain.document.entity.Document
import com.biuea.wiki.domain.document.entity.DocumentRevision
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Entity
@Table(name = "ai_agent_log")
@EntityListeners(AuditingEntityListener::class)
class AiAgentLog(
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type")
    val agentType: AgentType,

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    val status: AgentLogStatus,

    @Column(name = "action_detail")
    val actionDetail: String,

    @Column(name = "reference_data")
    val referenceData: String?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_revision_id")
    val documentRevisionId: DocumentRevision,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    val documentId: Document,

    @Column(name = "executor_id")
    val executorId: Long,

    @Column(name = "created_at")
    var createdAt: ZonedDateTime = ZonedDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
)
