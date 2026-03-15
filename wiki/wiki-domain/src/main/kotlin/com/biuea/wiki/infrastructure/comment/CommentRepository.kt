package com.biuea.wiki.infrastructure.comment

import com.biuea.wiki.domain.comment.Comment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CommentRepository : JpaRepository<Comment, Long> {
    @Query("SELECT c FROM Comment c WHERE c.documentId = :documentId ORDER BY c.createdAt ASC")
    fun findAllByDocumentId(@Param("documentId") documentId: Long): List<Comment>
}
