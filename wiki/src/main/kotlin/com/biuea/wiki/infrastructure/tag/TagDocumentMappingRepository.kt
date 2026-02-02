package com.biuea.wiki.infrastructure.tag

import com.biuea.wiki.domain.tag.entity.TagDocumentMapping
import org.springframework.data.jpa.repository.JpaRepository

interface TagDocumentMappingRepository: JpaRepository<TagDocumentMapping, Long> {
}