package com.closet.display.domain.repository

import com.closet.display.domain.entity.ExhibitionProduct
import org.springframework.data.jpa.repository.JpaRepository

interface ExhibitionProductRepository : JpaRepository<ExhibitionProduct, Long> {

    fun findByExhibitionIdOrderBySortOrderAsc(exhibitionId: Long): List<ExhibitionProduct>
}
