package com.biuea.wiki.presentation.search.request

data class VectorSearchRequest(
    val embedding: List<Float>? = null,
    val query: String? = null,
    val limit: Int = 10,
)
