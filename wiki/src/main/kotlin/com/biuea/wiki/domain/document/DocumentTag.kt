package com.biuea.wiki.domain.document

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "document_tag")
@EntityListeners(AuditingEntityListener::class)
class DocumentTag(
    @Column(nullable = false, columnDefinition = "TEXT")
    val name: String,

    @Column(name = "document_revision_id", nullable = false)
    val documentRevisionId: Long,

    @Column(name = "document_id", nullable = false)
    val documentId: Long,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
)
