package com.closet.search.infrastructure.repository

import com.closet.search.domain.ProductDocument
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

/**
 * Spring Data Elasticsearch 기본 CRUD 레포지토리.
 * 벌크 인덱싱과 기본 조회에 사용한다.
 */
interface ProductSearchRepository : ElasticsearchRepository<ProductDocument, Long>
