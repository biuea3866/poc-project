package com.biuea.wiki.infrastructure.tag

import com.biuea.wiki.domain.tag.entity.TagConstant
import com.biuea.wiki.domain.tag.entity.TagType
import org.springframework.data.jpa.repository.JpaRepository

interface TagTypeRepository: JpaRepository<TagType, Long> {
    fun existsByTagConstant(tagConstant: TagConstant): Boolean
    fun findByTagConstant(tagConstant: TagConstant): TagType?
}