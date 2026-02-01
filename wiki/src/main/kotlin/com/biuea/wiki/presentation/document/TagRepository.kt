package com.biuea.wiki.presentation.document

import com.biuea.wiki.domain.document.Tag
import com.biuea.wiki.domain.document.TagType
import org.springframework.data.jpa.repository.JpaRepository

interface TagRepository : JpaRepository<Tag, Long> {
    fun findByNameAndTagType(name: String, tagType: TagType): Tag?
}
