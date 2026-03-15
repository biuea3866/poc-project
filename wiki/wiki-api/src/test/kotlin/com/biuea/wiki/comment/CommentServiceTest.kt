package com.biuea.wiki.comment

import com.biuea.wiki.domain.comment.Comment
import com.biuea.wiki.infrastructure.comment.CommentRepository
import com.biuea.wiki.presentation.comment.CommentService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class CommentServiceTest {

    private val commentRepository: CommentRepository = mock()
    private val commentService = CommentService(commentRepository)

    // ─────────────────────────────────────────────────
    // getComments
    // ─────────────────────────────────────────────────

    @Test
    fun `getComments - 루트 댓글과 대댓글을 트리 구조로 반환한다`() {
        val root = comment(id = 1L, documentId = 10L, parentId = null)
        val reply = comment(id = 2L, documentId = 10L, parentId = 1L)
        whenever(commentRepository.findAllByDocumentId(10L)).thenReturn(listOf(root, reply))

        val result = commentService.getComments(10L)

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(1, result[0].replies.size)
        assertEquals(2L, result[0].replies[0].id)
    }

    @Test
    fun `getComments - 삭제된 댓글은 플레이스홀더 텍스트를 반환한다`() {
        val deleted = comment(id = 1L, documentId = 10L, content = "원래 내용", isDeleted = true)
        whenever(commentRepository.findAllByDocumentId(10L)).thenReturn(listOf(deleted))

        val result = commentService.getComments(10L)

        assertEquals("삭제된 댓글입니다.", result[0].content)
        assertTrue(result[0].isDeleted)
    }

    // ─────────────────────────────────────────────────
    // createComment
    // ─────────────────────────────────────────────────

    @Test
    fun `createComment - 루트 댓글을 정상 생성한다`() {
        val saved = comment(id = 1L, documentId = 10L, authorId = 99L, content = "안녕", parentId = null)
        whenever(commentRepository.save(any())).thenReturn(saved)

        val result = commentService.createComment(10L, 99L, "안녕", null)

        assertEquals(1L, result.id)
        assertNull(result.parentId)
    }

    @Test
    fun `createComment - 대댓글을 정상 생성한다`() {
        val parent = comment(id = 1L, documentId = 10L, parentId = null)
        val savedReply = comment(id = 2L, documentId = 10L, parentId = 1L)
        whenever(commentRepository.findById(1L)).thenReturn(Optional.of(parent))
        whenever(commentRepository.save(any())).thenReturn(savedReply)

        val result = commentService.createComment(10L, 99L, "대댓글", 1L)

        assertEquals(1L, result.parentId)
    }

    @Test
    fun `createComment - 대댓글의 대댓글 생성 시 예외를 던진다`() {
        val parentReply = comment(id = 2L, documentId = 10L, parentId = 1L)
        whenever(commentRepository.findById(2L)).thenReturn(Optional.of(parentReply))

        assertThrows<IllegalArgumentException> {
            commentService.createComment(10L, 99L, "3단계 댓글", 2L)
        }
    }

    // ─────────────────────────────────────────────────
    // updateComment
    // ─────────────────────────────────────────────────

    @Test
    fun `updateComment - 작성자가 댓글을 수정한다`() {
        val original = comment(id = 1L, authorId = 99L, content = "원본")
        whenever(commentRepository.findById(1L)).thenReturn(Optional.of(original))
        whenever(commentRepository.save(any())).thenAnswer { it.arguments[0] as Comment }

        val result = commentService.updateComment(1L, 99L, "수정됨")

        assertEquals("수정됨", result.content)
    }

    @Test
    fun `updateComment - 다른 사용자가 수정 시 예외를 던진다`() {
        val comment = comment(id = 1L, authorId = 99L)
        whenever(commentRepository.findById(1L)).thenReturn(Optional.of(comment))

        assertThrows<IllegalArgumentException> {
            commentService.updateComment(1L, authorId = 88L, content = "불법 수정")
        }
    }

    // ─────────────────────────────────────────────────
    // deleteComment
    // ─────────────────────────────────────────────────

    @Test
    fun `deleteComment - 작성자가 댓글을 소프트 삭제한다`() {
        val existing = comment(id = 1L, authorId = 99L)
        whenever(commentRepository.findById(1L)).thenReturn(Optional.of(existing))
        whenever(commentRepository.save(any())).thenAnswer { it.arguments[0] as Comment }

        commentService.deleteComment(1L, 99L)

        val captor = argumentCaptor<Comment>()
        verify(commentRepository).save(captor.capture())
        assertTrue(captor.firstValue.isDeleted)
        assertNotNull(captor.firstValue.deletedAt)
    }

    @Test
    fun `deleteComment - 다른 사용자가 삭제 시 예외를 던진다`() {
        val existing = comment(id = 1L, authorId = 99L)
        whenever(commentRepository.findById(1L)).thenReturn(Optional.of(existing))

        assertThrows<IllegalArgumentException> {
            commentService.deleteComment(1L, authorId = 88L)
        }
    }

    // ─────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────

    private fun comment(
        id: Long = 1L,
        documentId: Long = 10L,
        authorId: Long = 99L,
        content: String = "테스트 댓글",
        parentId: Long? = null,
        isDeleted: Boolean = false,
    ) = Comment(
        id = id,
        documentId = documentId,
        authorId = authorId,
        content = content,
        parentId = parentId,
        isDeleted = isDeleted,
    )
}
