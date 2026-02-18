package com.biuea.wiki.domain.tag.rule

import com.biuea.wiki.domain.tag.entity.Tag
import com.biuea.wiki.domain.tag.entity.TagConstant
import com.biuea.wiki.domain.tag.entity.TagType
import com.biuea.wiki.domain.tag.rule.TagRule.Companion.MAX_NAME_LENGTH
import com.biuea.wiki.infrastructure.tag.TagRepository
import com.biuea.wiki.infrastructure.tag.TagTypeRepository

interface TagRule {
    fun validate(tag: Tag)

    companion object {
        const val MAX_NAME_LENGTH = 50
    }
}

class DefaultTagRule: TagRule {
    override fun validate(tag: Tag) {
        if (tag.name.length > MAX_NAME_LENGTH ) throw IllegalArgumentException()
    }
}