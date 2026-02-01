package com.biuea.wiki.presentation.document

import com.biuea.wiki.domain.document.entity.TagType
import org.springframework.data.jpa.repository.JpaRepository

interface TagTypeRepository : JpaRepository<TagType, Long> {
    fun findByName(name: String): TagType?

    fun findByNameIn(names: Set<String>): List<TagType>
}
