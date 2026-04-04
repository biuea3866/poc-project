package com.closet.search.infrastructure.repository

import com.closet.search.application.dto.ProductSearchFilter
import com.closet.search.domain.ProductDocument
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHitSupport
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.stereotype.Repository
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType
import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.json.JsonData

private val logger = KotlinLogging.logger {}

/**
 * Elasticsearch 커스텀 검색 구현체.
 *
 * nori 한글 분석기를 활용한 full-text 검색과
 * edge_ngram 분석기를 활용한 자동완성 검색을 제공한다.
 */
@Repository
class ProductSearchRepositoryImpl(
    private val operations: ElasticsearchOperations,
) : ProductSearchRepositoryCustom {

    override fun search(filter: ProductSearchFilter, pageable: Pageable): Page<ProductDocument> {
        val boolQuery = BoolQuery.Builder()

        // 키워드 검색 (nori 분석기 활용 multi_match)
        if (!filter.keyword.isNullOrBlank()) {
            boolQuery.must { m ->
                m.multiMatch(
                    MultiMatchQuery.Builder()
                        .query(filter.keyword)
                        .fields("name^3", "description", "brandName^2", "categoryName")
                        .type(TextQueryType.BestFields)
                        .build()
                )
            }
        }

        // 카테고리 필터
        if (!filter.category.isNullOrBlank()) {
            boolQuery.filter { f ->
                f.term(TermQuery.Builder().field("categoryName").value(filter.category).build())
            }
        }

        // 브랜드 필터
        if (!filter.brand.isNullOrBlank()) {
            boolQuery.filter { f ->
                f.term(TermQuery.Builder().field("brandName").value(filter.brand).build())
            }
        }

        // 가격 범위 필터
        if (filter.minPrice != null || filter.maxPrice != null) {
            boolQuery.filter { f ->
                f.range { r ->
                    val rangeBuilder = r.field("salePrice")
                    if (filter.minPrice != null) {
                        rangeBuilder.gte(JsonData.of(filter.minPrice))
                    }
                    if (filter.maxPrice != null) {
                        rangeBuilder.lte(JsonData.of(filter.maxPrice))
                    }
                    rangeBuilder
                }
            }
        }

        // 사이즈 필터
        if (!filter.sizes.isNullOrEmpty()) {
            boolQuery.filter { f ->
                f.terms(
                    TermsQuery.Builder()
                        .field("sizes")
                        .terms { t -> t.value(filter.sizes.map { FieldValue.of(it) }) }
                        .build()
                )
            }
        }

        // 색상 필터
        if (!filter.colors.isNullOrEmpty()) {
            boolQuery.filter { f ->
                f.terms(
                    TermsQuery.Builder()
                        .field("colors")
                        .terms { t -> t.value(filter.colors.map { FieldValue.of(it) }) }
                        .build()
                )
            }
        }

        // 성별 필터
        if (!filter.gender.isNullOrBlank()) {
            boolQuery.filter { f ->
                f.term(TermQuery.Builder().field("gender").value(filter.gender).build())
            }
        }

        // 시즌 필터
        if (!filter.season.isNullOrBlank()) {
            boolQuery.filter { f ->
                f.term(TermQuery.Builder().field("season").value(filter.season).build())
            }
        }

        // 핏 타입 필터
        if (!filter.fitType.isNullOrBlank()) {
            boolQuery.filter { f ->
                f.term(TermQuery.Builder().field("fitType").value(filter.fitType).build())
            }
        }

        // 상태 필터 (기본: ACTIVE만 검색)
        boolQuery.filter { f ->
            f.term(TermQuery.Builder().field("status").value(filter.status ?: "ACTIVE").build())
        }

        val nativeQueryBuilder = NativeQuery.builder()
            .withQuery { q -> q.bool(boolQuery.build()) }
            .withPageable(pageable)

        // 정렬
        when (filter.sort) {
            "price_asc" -> nativeQueryBuilder.withSort { s -> s.field { f -> f.field("salePrice").order(SortOrder.Asc) } }
            "price_desc" -> nativeQueryBuilder.withSort { s -> s.field { f -> f.field("salePrice").order(SortOrder.Desc) } }
            "newest" -> nativeQueryBuilder.withSort { s -> s.field { f -> f.field("createdAt").order(SortOrder.Desc) } }
            "popularity" -> nativeQueryBuilder.withSort { s -> s.field { f -> f.field("popularityScore").order(SortOrder.Desc) } }
            "review" -> nativeQueryBuilder.withSort { s -> s.field { f -> f.field("reviewCount").order(SortOrder.Desc) } }
            "discount" -> nativeQueryBuilder.withSort { s -> s.field { f -> f.field("discountRate").order(SortOrder.Desc) } }
            else -> {
                // 기본 정렬: 인기순 -> 최신순
                nativeQueryBuilder.withSort { s -> s.field { f -> f.field("popularityScore").order(SortOrder.Desc) } }
                nativeQueryBuilder.withSort { s -> s.field { f -> f.field("createdAt").order(SortOrder.Desc) } }
            }
        }

        val query: Query = nativeQueryBuilder.build()
        val searchHits = operations.search(query, ProductDocument::class.java)

        logger.debug { "검색 결과: totalHits=${searchHits.totalHits}, keyword=${filter.keyword}" }

        @Suppress("UNCHECKED_CAST")
        return SearchHitSupport.searchPageFor(searchHits, pageable) as Page<ProductDocument>
    }

    override fun autocomplete(keyword: String, size: Int): List<ProductDocument> {
        if (keyword.isBlank()) return emptyList()

        val query = NativeQuery.builder()
            .withQuery { q ->
                q.bool { b ->
                    b.should { s ->
                        s.match(
                            MatchQuery.Builder()
                                .field("name.autocomplete")
                                .query(keyword)
                                .build()
                        )
                    }
                    b.should { s ->
                        s.match(
                            MatchQuery.Builder()
                                .field("brandName.autocomplete")
                                .query(keyword)
                                .build()
                        )
                    }
                    b.filter { f ->
                        f.term(TermQuery.Builder().field("status").value("ACTIVE").build())
                    }
                    b.minimumShouldMatch("1")
                }
            }
            .withMaxResults(size)
            .build()

        val searchHits = operations.search(query, ProductDocument::class.java)
        return searchHits.searchHits.map { it.content }
    }
}
