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

@Entity
@Table(name = "ai_agent_log")
@EntityListeners(AuditingEntityListener::class)
class AiAgentLog(
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false, length = 50)
    val agentType: AgentType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val status: AgentLogStatus,

    @Column(name = "action_detail", nullable = false, columnDefinition = "TEXT")
    val actionDetail: String,

    @Column(name = "reference_data", columnDefinition = "TEXT")
    val referenceData: String? = null,

    @Column(name = "document_revision_id", nullable = false)
    val documentRevisionId: Long,

    @Column(name = "document_id", nullable = false)
    val documentId: Long,

    @Column(name = "executor_id", nullable = false)
    val executorId: Long,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
)
