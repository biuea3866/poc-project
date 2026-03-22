package com.closet.display.domain.entity

import com.closet.display.domain.enums.PeriodType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import java.time.LocalDateTime

@Entity
@Table(name = "ranking_snapshot")
class RankingSnapshot(

    @Column(name = "category_id", nullable = false)
    val categoryId: Long,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "rank_position", nullable = false)
    val rankPosition: Int,

    @Column(name = "score", nullable = false)
    val score: Double,

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val periodType: PeriodType,

    @Column(name = "snapshot_date", nullable = false, columnDefinition = "DATETIME(6)")
    val snapshotDate: LocalDateTime

) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    var createdAt: LocalDateTime = LocalDateTime.now()
}
