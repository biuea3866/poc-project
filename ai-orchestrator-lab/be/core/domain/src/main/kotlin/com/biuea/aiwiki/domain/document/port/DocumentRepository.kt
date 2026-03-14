package com.biuea.aiwiki.domain.document.port

import com.biuea.aiwiki.domain.document.model.Document

interface DocumentRepository {
    fun save(document: Document): Document
    fun findById(id: Long): Document?
    fun searchActiveOwnedByUser(query: String, userId: Long): List<Document>
}
