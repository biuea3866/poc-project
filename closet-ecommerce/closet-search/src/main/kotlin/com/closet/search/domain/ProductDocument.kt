package com.closet.search.domain

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

@Document(indexName = "products")
data class ProductDocument(
    @Id
    val id: Long,

    @Field(type = FieldType.Text, analyzer = "standard")
    val name: String,

    @Field(type = FieldType.Text, analyzer = "standard")
    val description: String?,

    @Field(type = FieldType.Long)
    val brandId: Long,

    @Field(type = FieldType.Keyword)
    val brandName: String?,

    @Field(type = FieldType.Long)
    val categoryId: Long,

    @Field(type = FieldType.Keyword)
    val categoryName: String?,

    @Field(type = FieldType.Long)
    val basePrice: Long,

    @Field(type = FieldType.Long)
    val salePrice: Long,

    @Field(type = FieldType.Integer)
    val discountRate: Int,

    @Field(type = FieldType.Keyword)
    val status: String,

    @Field(type = FieldType.Keyword)
    val season: String,

    @Field(type = FieldType.Keyword)
    val fitType: String,

    @Field(type = FieldType.Keyword)
    val gender: String,

    @Field(type = FieldType.Keyword)
    val createdAt: String?,
)
