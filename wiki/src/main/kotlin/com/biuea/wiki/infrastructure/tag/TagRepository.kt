package com.biuea.wiki.infrastructure.tag

import com.biuea.wiki.domain.tag.entity.Tag
import com.biuea.wiki.domain.tag.entity.TagType
import org.springframework.data.jpa.repository.JpaRepository

interface TagRepository : JpaRepository<Tag, Long> {
    fun findByNameAndTagType(name: String, tagType: TagType): Tag?
}