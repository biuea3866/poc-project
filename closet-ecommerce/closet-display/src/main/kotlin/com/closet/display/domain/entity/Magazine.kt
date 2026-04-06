package com.closet.display.domain.entity

import com.closet.common.entity.BaseEntity
import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "magazine")
class Magazine(
    @Column(name = "title", nullable = false, length = 200)
    var title: String,
    @Column(name = "subtitle", length = 300)
    var subtitle: String? = null,
    @Column(name = "content_body", nullable = false, columnDefinition = "TEXT")
    var contentBody: String,
    @Column(name = "thumbnail_url", length = 500)
    var thumbnailUrl: String? = null,
    @Column(name = "category", nullable = false, length = 50)
    var category: String,
    @Column(name = "author_name", nullable = false, length = 50)
    var authorName: String,
    @Column(name = "view_count", nullable = false)
    var viewCount: Long = 0,
    @Column(name = "is_published", nullable = false, columnDefinition = "TINYINT(1)")
    var isPublished: Boolean = false,
    @Column(name = "published_at", columnDefinition = "DATETIME(6)")
    var publishedAt: ZonedDateTime? = null,
    @OneToMany(mappedBy = "magazine", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val products: MutableList<MagazineProduct> = mutableListOf(),
    @OneToMany(mappedBy = "magazine", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val tags: MutableList<MagazineTag> = mutableListOf(),
) : BaseEntity() {
    fun publish() {
        if (isPublished) {
            throw BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "이미 발행된 매거진입니다")
        }
        isPublished = true
        publishedAt = ZonedDateTime.now()
    }

    fun unpublish() {
        if (!isPublished) {
            throw BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "발행 상태가 아닌 매거진입니다")
        }
        isPublished = false
    }

    fun incrementViewCount() {
        viewCount++
    }

    fun update(
        title: String,
        subtitle: String?,
        contentBody: String,
        thumbnailUrl: String?,
        category: String,
        authorName: String,
    ) {
        this.title = title
        this.subtitle = subtitle
        this.contentBody = contentBody
        this.thumbnailUrl = thumbnailUrl
        this.category = category
        this.authorName = authorName
    }

    fun addProduct(product: MagazineProduct) {
        product.magazine = this
        products.add(product)
    }

    fun removeProduct(productId: Long) {
        val product =
            products.find { it.productId == productId }
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "매거진 상품을 찾을 수 없습니다: productId=$productId")
        products.remove(product)
    }

    fun addTag(tag: MagazineTag) {
        tag.magazine = this
        tags.add(tag)
    }

    fun removeTag(tagName: String) {
        val tag =
            tags.find { it.tagName == tagName }
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "매거진 태그를 찾을 수 없습니다: tagName=$tagName")
        tags.remove(tag)
    }
}
