package com.biuea.aiwiki.domain.document.model

import java.time.Instant

class DocumentRevision(
    val id: Long? = null,
    val documentId: Long,
    title: String,
    content: String,
    status: DocumentStatus,
    val createdBy: Long,
    val createdAt: Instant? = null,
) {
    val title: String = title
    val content: String = content
    val status: DocumentStatus = status
}
