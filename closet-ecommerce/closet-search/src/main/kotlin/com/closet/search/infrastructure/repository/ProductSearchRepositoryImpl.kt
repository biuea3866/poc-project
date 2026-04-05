package com.closet.search.infrastructure.repository

import com.closet.search.application.dto.FacetBucket
import com.closet.search.application.dto.FacetResult
import com.closet.search.application.dto.FilterFacetResponse
import com.closet.search.application.dto.PriceRangeBucket
import com.closet.search.application.dto.ProductSearchFilter
import com.closet.search.application.dto.ProductSearchResponse
import com.closet.search.domain.ProductDocument
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHitSupport
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.stereotype.Repository
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery
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
 * nori 한글 분석기를 활용한 full-text 검색,
 * edge_ngram 분석기를 활용한 자동완성 검색,
 * Aggregation을 활용한 facet 필터를 제공한다.
 */
@Repository
class ProductSearchRepositoryImpl(
    private val operations: ElasticsearchOperations,
) : ProductSearchRepositoryCustom {

    override fun search(filter: ProductSearchFilter, pageable: Pageable): Page<ProductDocument> {
        val boolQuery = buildBoolQuery(filter)

        val nativeQueryBuilder = NativeQuery.builder()
            .withQuery { q -> q.bool(boolQuery.build()) }
            .withPageable(pageable)

        applySorting(nativeQueryBuilder, filter.sort)

        val query: Query = nativeQueryBuilder.build()
        val searchHits = operations.search(query, ProductDocument::class.java)

        logger.debug { "검색 결과: totalHits=${searchHits.totalHits}, keyword=${filter.keyword}" }

        @Suppress("UNCHECKED_CAST")
        return SearchHitSupport.searchPageFor(searchHits, pageable) as Page<ProductDocument>
    }

    override fun searchWithFacets(filter: ProductSearchFilter, pageable: Pageable): FilterFacetResponse {
        val boolQuery = buildBoolQuery(filter)

        val nativeQueryBuilder = NativeQuery.builder()
            .withQuery { q -> q.bool(boolQuery.build()) }
            .withPageable(pageable)

        applySorting(nativeQueryBuilder, filter.sort)

        // Aggregation 추가 (카테고리, 브랜드, 사이즈, 색상, 가격 범위)
        nativeQueryBuilder.withAggregation(
            "categories",
            Aggregation.of { a -> a.terms { t -> t.field("categoryName").size(50) } }
        )
        nativeQueryBuilder.withAggregation(
            "brands",
            Aggregation.of { a -> a.terms { t -> t.field("brandName").size(100) } }
        )
        nativeQueryBuilder.withAggregation(
            "sizes",
            Aggregation.of { a -> a.terms { t -> t.field("sizes").size(30) } }
        )
        nativeQueryBuilder.withAggregation(
            "colors",
            Aggregation.of { a -> a.terms { t -> t.field("colors").size(50) } }
        )
        nativeQueryBuilder.withAggregation(
            "price_ranges",
            Aggregation.of { a ->
                a.range { r ->
                    r.field("salePrice")
                        .ranges({ rr -> rr.to("10000") })
                        .ranges({ rr -> rr.from("10000").to("30000") })
                        .ranges({ rr -> rr.from("30000").to("50000") })
                        .ranges({ rr -> rr.from("50000").to("100000") })
                        .ranges({ rr -> rr.from("100000").to("200000") })
                        .ranges({ rr -> rr.from("200000") })
                }
            }
        )

        val query: Query = nativeQueryBuilder.build()
        val searchHits = operations.search(query, ProductDocument::class.java)

        logger.debug { "필터 검색 결과: totalHits=${searchHits.totalHits}, keyword=${filter.keyword}" }

        // 상품 목록 매핑
        val products = searchHits.searchHits.map { ProductSearchResponse.from(it.content) }

        // Aggregation 결과 파싱
        val aggregations = searchHits.aggregations

        val categories = extractTermBuckets(aggregations, "categories")
        val brands = extractTermBuckets(aggregations, "brands")
        val sizes = extractTermBuckets(aggregations, "sizes")
        val colors = extractTermBuckets(aggregations, "colors")
        val priceRanges = extractRangeBuckets(aggregations, "price_ranges")

        val totalHits = searchHits.totalHits
        val totalPages = if (pageable.pageSize > 0) {
            ((totalHits + pageable.pageSize - 1) / pageable.pageSize).toInt()
        } else 0

        return FilterFacetResponse(
            products = products,
            totalElements = totalHits,
            totalPages = totalPages,
            page = pageable.pageNumber,
            size = pageable.pageSize,
            facets = FacetResult(
                categories = categories,
                brands = brands,
                sizes = sizes,
                colors = colors,
                priceRanges = priceRanges,
            ),
        )
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
                    b.should { s ->
                        s.match(
                            MatchQuery.Builder()
                                .field("categoryName.autocomplete")
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

    // ──────────────────────────── 공통 빌더 메서드 ────────────────────────────

    /**
     * 검색 필터를 기반으로 BoolQuery를 빌드한다.
     */
    private fun buildBoolQuery(filter: ProductSearchFilter): BoolQuery.Builder {
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

        return boolQuery
    }

    /**
     * 정렬 옵션 적용.
     */
    private fun applySorting(builder: NativeQuery.Builder, sort: String?) {
        when (sort) {
            "PRICE_ASC", "price_asc" -> builder.withSort { s -> s.field { f -> f.field("salePrice").order(SortOrder.Asc) } }
            "PRICE_DESC", "price_desc" -> builder.withSort { s -> s.field { f -> f.field("salePrice").order(SortOrder.Desc) } }
            "LATEST", "newest" -> builder.withSort { s -> s.field { f -> f.field("createdAt").order(SortOrder.Desc) } }
            "POPULAR", "popularity" -> builder.withSort { s -> s.field { f -> f.field("popularityScore").order(SortOrder.Desc) } }
            "review" -> builder.withSort { s -> s.field { f -> f.field("reviewCount").order(SortOrder.Desc) } }
            "discount" -> builder.withSort { s -> s.field { f -> f.field("discountRate").order(SortOrder.Desc) } }
            "RELEVANCE", null -> {
                // 기본 정렬: 관련도(_score) -> 인기순 -> 최신순
                builder.withSort { s -> s.field { f -> f.field("popularityScore").order(SortOrder.Desc) } }
                builder.withSort { s -> s.field { f -> f.field("createdAt").order(SortOrder.Desc) } }
            }
            else -> {
                builder.withSort { s -> s.field { f -> f.field("popularityScore").order(SortOrder.Desc) } }
                builder.withSort { s -> s.field { f -> f.field("createdAt").order(SortOrder.Desc) } }
            }
        }
    }

    // ──────────────────────────── Aggregation 파싱 ────────────────────────────

    /**
     * Terms Aggregation 결과를 FacetBucket 리스트로 변환한다.
     */
    @Suppress("UNCHECKED_CAST")
    private fun extractTermBuckets(aggregations: Any?, name: String): List<FacetBucket> {
        if (aggregations == null) return emptyList()

        return try {
            val aggMap = aggregations as? org.springframework.data.elasticsearch.core.AggregationsContainer<*>
                ?: return emptyList()

            val esAggregations = aggMap.aggregations() as? Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate>
                ?: return emptyList()

            val agg = esAggregations[name] ?: return emptyList()

            val sterms = agg.sterms()
            sterms.buckets().array().map { bucket ->
                FacetBucket(
                    key = bucket.key().stringValue(),
                    count = bucket.docCount(),
                )
            }
        } catch (e: Exception) {
            logger.warn(e) { "Aggregation '$name' 파싱 실패" }
            emptyList()
        }
    }

    /**
     * Range Aggregation 결과를 PriceRangeBucket 리스트로 변환한다.
     */
    @Suppress("UNCHECKED_CAST")
    private fun extractRangeBuckets(aggregations: Any?, name: String): List<PriceRangeBucket> {
        if (aggregations == null) return emptyList()

        return try {
            val aggMap = aggregations as? org.springframework.data.elasticsearch.core.AggregationsContainer<*>
                ?: return emptyList()

            val esAggregations = aggMap.aggregations() as? Map<String, co.elastic.clients.elasticsearch._types.aggregations.Aggregate>
                ?: return emptyList()

            val agg = esAggregations[name] ?: return emptyList()

            val rangeAgg = agg.range()
            rangeAgg.buckets().array().map { bucket ->
                PriceRangeBucket(
                    from = if (bucket.from() != null && !bucket.from().isInfinite()) bucket.from() else null,
                    to = if (bucket.to() != null && !bucket.to().isInfinite()) bucket.to() else null,
                    count = bucket.docCount(),
                )
            }
        } catch (e: Exception) {
            logger.warn(e) { "Aggregation '$name' 파싱 실패" }
            emptyList()
        }
    }
}
