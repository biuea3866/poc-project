package com.biuea.wiki.presentation.tag

import com.biuea.wiki.domain.tag.entity.TagConstant
import com.biuea.wiki.presentation.tag.response.TagTypeResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/tags")
class TagApiController {

    @GetMapping("/types")
    fun getTagTypes(): ResponseEntity<TagTypeResponse> {
        return ResponseEntity.ok(TagTypeResponse.from(TagConstant.entries))
    }
}
