package com.closet.search.infrastructure.repository

import com.closet.search.domain.ProductDocument
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

interface ProductSearchRepository : ElasticsearchRepository<ProductDocument, Long>
