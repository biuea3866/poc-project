package com.closet.cs.domain.repository

import com.closet.cs.domain.Faq
import com.closet.cs.domain.FaqCategory
import org.springframework.data.jpa.repository.JpaRepository

interface FaqRepository : JpaRepository<Faq, Long> {
    fun findByCategoryAndIsVisibleTrueOrderBySortOrderAsc(category: FaqCategory): List<Faq>

    fun findByIsVisibleTrueOrderBySortOrderAsc(): List<Faq>
}
