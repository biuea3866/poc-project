package com.biuea.wiki.domain.document.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class DocumentTest {

    private fun createDocument(
        title: String = "Test Document",
        content: String? = "Test Content",
        status: DocumentStatus = DocumentStatus.PENDING,
    ): Document {
        return Document(
            title = title,
            content = content,
            status = status,
            createdBy = 1L,
            updatedBy = 1L,
        )
    }

    @Test
    fun `softDelete sets status to DELETED and deletedAt`() {
        val document = createDocument()

        document.softDelete()

        assertEquals(DocumentStatus.DELETED, document.status)
        assertTrue(document.isDeleted())
        assertFalse(document.deletedAt == null)
    }

    @Test
    fun `softDelete cascades to children`() {
        val parent = createDocument(title = "Parent")
        val child = createDocument(title = "Child")
        parent.addChild(child)

        parent.softDelete()

        assertTrue(parent.isDeleted())
        assertTrue(child.isDeleted())
    }

    @Test
    fun `restore resets status to PENDING and clears deletedAt`() {
        val document = createDocument()
        document.softDelete()

        document.restore()

        assertEquals(DocumentStatus.PENDING, document.status)
        assertFalse(document.isDeleted())
        assertNull(document.deletedAt)
    }

    @Test
    fun `restore throws if document is not deleted`() {
        val document = createDocument()

        assertFailsWith<IllegalStateException> {
            document.restore()
        }
    }

    @Test
    fun `restore detaches from deleted parent`() {
        val parent = createDocument(title = "Parent")
        val child = createDocument(title = "Child")
        parent.addChild(child)
        parent.softDelete()

        child.restore()

        assertNull(child.parent)
        assertFalse(child.isDeleted())
    }

    @Test
    fun `restore keeps non-deleted parent`() {
        val parent = createDocument(title = "Parent")
        val child = createDocument(title = "Child")
        parent.addChild(child)
        child.softDelete()

        child.restore()

        assertEquals(parent, child.parent)
    }

    @Test
    fun `publish sets status to COMPLETED`() {
        val document = createDocument()

        document.publish()

        assertEquals(DocumentStatus.COMPLETED, document.status)
    }

    @Test
    fun `update changes title, content, and updatedBy`() {
        val document = createDocument()

        document.update("New Title", "New Content", 2L)

        assertEquals("New Title", document.title)
        assertEquals("New Content", document.content)
        assertEquals(2L, document.updatedBy)
    }

    @Test
    fun `update with null content`() {
        val document = createDocument()

        document.update("New Title", null, 2L)

        assertEquals("New Title", document.title)
        assertNull(document.content)
    }
}
