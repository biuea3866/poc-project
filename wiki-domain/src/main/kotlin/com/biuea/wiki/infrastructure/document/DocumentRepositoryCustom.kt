package com.biuea.wiki.infrastructure.document

import com.biuea.wiki.domain.document.entity.Document
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface DocumentRepositoryCustom {
    fun searchByKeyword(keyword: String, pageable: Pageable): Page<Document>
}
