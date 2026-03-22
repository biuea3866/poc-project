package com.closet.search.infrastructure.repository

import com.closet.search.application.dto.ProductSearchFilter
import com.closet.search.domain.ProductDocument
import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHits
import org.springframework.stereotype.Repository

private val logger = KotlinLogging.logger {}

@Repository
class ProductSearchRepositoryImpl(
    private val operations: ElasticsearchOperations,
) : ProductSearchRepositoryCustom {

    override fun search(keyword: String?, filter: ProductSearchFilter, pageable: Pageable): Page<ProductDocument> {
        val queryBuilder = BoolQuery.Builder()

        // keyword search on name and description
        if (!keyword.isNullOrBlank()) {
            queryBuilder.must { m ->
                m.multiMatch { mm ->
                    mm.query(keyword)
                        .fields("name^2", "description")
                }
            }
        }

        // filters
        filter.categoryId?.let { categoryId ->
            queryBuilder.filter { f -> f.term { t -> t.field("categoryId").value(categoryId) } }
        }
        filter.brandId?.let { brandId ->
            queryBuilder.filter { f -> f.term { t -> t.field("brandId").value(brandId) } }
        }
        filter.minPrice?.let { minPrice ->
            queryBuilder.filter { f ->
                f.range { r ->
                    r.field("salePrice")
                        .gte(co.elastic.clients.json.JsonData.of(minPrice))
                }
            }
        }
        filter.maxPrice?.let { maxPrice ->
            queryBuilder.filter { f ->
                f.range { r ->
                    r.field("salePrice")
                        .lte(co.elastic.clients.json.JsonData.of(maxPrice))
                }
            }
        }
        filter.gender?.let { gender ->
            queryBuilder.filter { f -> f.term { t -> t.field("gender").value(gender) } }
        }
        filter.season?.let { season ->
            queryBuilder.filter { f -> f.term { t -> t.field("season").value(season) } }
        }

        // only ACTIVE products
        queryBuilder.filter { f -> f.term { t -> t.field("status").value("ACTIVE") } }

        val nativeQueryBuilder = NativeQuery.builder()
            .withQuery(Query.Builder().bool(queryBuilder.build()).build())
            .withPageable(pageable)

        // sorting
        when (filter.sort) {
            "price_asc" -> nativeQueryBuilder.withSort { s -> s.field { f -> f.field("salePrice").order(SortOrder.Asc) } }
            "price_desc" -> nativeQueryBuilder.withSort { s -> s.field { f -> f.field("salePrice").order(SortOrder.Desc) } }
            "discount" -> nativeQueryBuilder.withSort { s -> s.field { f -> f.field("discountRate").order(SortOrder.Desc) } }
            "newest" -> nativeQueryBuilder.withSort { s -> s.field { f -> f.field("createdAt").order(SortOrder.Desc) } }
            else -> {
                if (!keyword.isNullOrBlank()) {
                    nativeQueryBuilder.withSort { s -> s.score { sc -> sc.order(SortOrder.Desc) } }
                } else {
                    nativeQueryBuilder.withSort { s -> s.field { f -> f.field("createdAt").order(SortOrder.Desc) } }
                }
            }
        }

        val query = nativeQueryBuilder.build()
        val searchHits: SearchHits<ProductDocument> = operations.search(query, ProductDocument::class.java)

        val documents = searchHits.searchHits.map { it.content }
        logger.info { "검색 결과: keyword=$keyword, totalHits=${searchHits.totalHits}, returned=${documents.size}" }

        return PageImpl(documents, pageable, searchHits.totalHits)
    }

    override fun autocomplete(prefix: String, limit: Int): List<String> {
        if (prefix.isBlank()) return emptyList()

        val query = NativeQuery.builder()
            .withQuery { q ->
                q.bool { b ->
                    b.must { m ->
                        m.prefix { p -> p.field("name").value(prefix.lowercase()) }
                    }
                    b.filter { f -> f.term { t -> t.field("status").value("ACTIVE") } }
                }
            }
            .withMaxResults(limit)
            .build()

        val searchHits = operations.search(query, ProductDocument::class.java)
        return searchHits.searchHits
            .map { it.content.name }
            .distinct()
    }
}
