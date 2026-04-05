package com.closet.search.domain

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.annotations.InnerField
import org.springframework.data.elasticsearch.annotations.MultiField
import org.springframework.data.elasticsearch.annotations.Setting
import java.math.BigDecimal
import java.time.ZonedDateTime

/**
 * Elasticsearch 상품 문서 모델.
 *
 * closet-products 인덱스에 매핑되며 nori 한글 분석기와 edge_ngram 자동완성을 지원한다.
 * 인덱스 세팅은 product-index-settings.json에서 관리한다.
 *
 * name, brandName, categoryName 필드는 autocomplete 서브필드를 갖고 있어
 * edge_ngram 기반 자동완성 검색에 활용된다.
 */
@Document(indexName = "closet-products")
@Setting(settingPath = "elasticsearch/product-index-settings.json")
data class ProductDocument(

    @Id
    val productId: Long,

    @MultiField(
        mainField = Field(type = FieldType.Text, analyzer = "nori_analyzer", searchAnalyzer = "nori_analyzer"),
        otherFields = [
            InnerField(suffix = "autocomplete", type = FieldType.Text, analyzer = "autocomplete_analyzer", searchAnalyzer = "autocomplete_search_analyzer"),
            InnerField(suffix = "keyword", type = FieldType.Keyword),
        ],
    )
    val name: String,

    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    val description: String,

    @Field(type = FieldType.Long)
    val brandId: Long,

    @MultiField(
        mainField = Field(type = FieldType.Keyword),
        otherFields = [
            InnerField(suffix = "autocomplete", type = FieldType.Text, analyzer = "autocomplete_analyzer", searchAnalyzer = "autocomplete_search_analyzer"),
        ],
    )
    val brandName: String? = null,

    @Field(type = FieldType.Long)
    val categoryId: Long,

    @MultiField(
        mainField = Field(type = FieldType.Keyword),
        otherFields = [
            InnerField(suffix = "autocomplete", type = FieldType.Text, analyzer = "autocomplete_analyzer", searchAnalyzer = "autocomplete_search_analyzer"),
        ],
    )
    val categoryName: String? = null,

    @Field(type = FieldType.Keyword)
    val subCategoryName: String? = null,

    @Field(type = FieldType.Double)
    val basePrice: BigDecimal,

    @Field(type = FieldType.Double)
    val salePrice: BigDecimal,

    @Field(type = FieldType.Integer)
    val discountRate: Int = 0,

    @Field(type = FieldType.Keyword)
    val sizes: List<String> = emptyList(),

    @Field(type = FieldType.Keyword)
    val colors: List<String> = emptyList(),

    @Field(type = FieldType.Keyword)
    val fitType: String? = null,

    @Field(type = FieldType.Keyword)
    val gender: String? = null,

    @Field(type = FieldType.Keyword)
    val season: String? = null,

    @Field(type = FieldType.Keyword)
    val status: String,

    @Field(type = FieldType.Keyword)
    val imageUrl: String? = null,

    @Field(type = FieldType.Double)
    val popularityScore: Double = 0.0,

    @Field(type = FieldType.Integer)
    val salesCount: Int = 0,

    @Field(type = FieldType.Integer)
    val reviewCount: Int = 0,

    @Field(type = FieldType.Double)
    val avgRating: Double = 0.0,

    @Field(type = FieldType.Integer)
    val viewCount: Int = 0,

    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second_millis, DateFormat.epoch_millis])
    val createdAt: ZonedDateTime? = null,

    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second_millis, DateFormat.epoch_millis])
    val updatedAt: ZonedDateTime? = null,
)
