package com.closet.product.domain.entity

import com.closet.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "category")
class Category(

    @Column(name = "parent_id")
    val parentId: Long? = null,

    @Column(name = "name", nullable = false, length = 50)
    var name: String,

    @Column(name = "depth", nullable = false)
    val depth: Int,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(name = "status", nullable = false, columnDefinition = "TINYINT(1)")
    var status: Boolean = true

) : BaseEntity()
