package com.biuea.wiki.presentation.document

import com.biuea.wiki.domain.document.Tag
import org.springframework.data.jpa.repository.JpaRepository

interface TagRepository : JpaRepository<Tag, Long> {
    fun findByNameIn(names: Set<String>): List<Tag>

    fun findByName(name: String): Tag?
}
