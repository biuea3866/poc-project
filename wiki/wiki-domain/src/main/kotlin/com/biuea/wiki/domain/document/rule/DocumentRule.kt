package com.biuea.wiki.domain.document.rule

import com.biuea.wiki.domain.document.entity.Document
import com.biuea.wiki.domain.document.rule.DocumentRule.Companion.MAX_TITLE_LENGTH

interface DocumentRule {
    fun validate(document: Document)

    companion object {
        const val MAX_TITLE_LENGTH = 255
    }
}

class DefaultDocumentRule: DocumentRule {
    override fun validate(document: Document) {
        if (document.title.length > MAX_TITLE_LENGTH) throw IllegalArgumentException("Title length is too long")
    }
}