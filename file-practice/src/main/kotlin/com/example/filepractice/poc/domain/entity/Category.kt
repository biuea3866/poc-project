package com.example.filepractice.poc.domain.entity

data class Category(
    val id: Int,
    val name: String,
    val parentCategoryId: Int?,
    val description: String?,
    val displayOrder: Int,
    val isActive: Boolean
)
