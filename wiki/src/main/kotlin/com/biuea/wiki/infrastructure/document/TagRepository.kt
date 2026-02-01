package com.biuea.wiki.presentation.document

import com.biuea.wiki.domain.document.entity.Tag
import com.biuea.wiki.domain.document.entity.TagType
import org.springframework.data.jpa.repository.JpaRepository

interface TagRepository : JpaRepository<Tag, Long> {
    fun findByNameAndTagType(name: String, tagType: TagType): Tag?
}
