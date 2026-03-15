package com.biuea.wiki.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID

class DocumentApiIntegrationTest(
    @Value("\${local.server.port}") private val port: Int,
) : BaseIntegrationTest() {
    @Autowired
    private lateinit var cacheManager: CacheManager

    private val httpClient: HttpClient = HttpClient.newHttpClient()
    private var accessToken: String = ""

    @BeforeEach
    fun setUp() {
        cacheManager.cacheNames.forEach { cacheManager.getCache(it)?.clear() }

        val email = "doc-test-${UUID.randomUUID()}@example.com"
        val signUpResponse = request(
            method = "POST",
            path = "/api/v1/auth/signup",
            body = """{"email":"$email","password":"password123!","name":"doc-tester"}""",
        )
        assertEquals(200, signUpResponse.statusCode())

        val loginResponse = request(
            method = "POST",
            path = "/api/v1/auth/login",
            body = """{"email":"$email","password":"password123!"}""",
        )
        assertEquals(200, loginResponse.statusCode())
        accessToken = extractJsonValue(loginResponse.body(), "accessToken")
        assertTrue(accessToken.isNotBlank())
    }

    @Test
    fun `document CRUD lifecycle`() {
        // CREATE
        val createResponse = request(
            method = "POST",
            path = "/api/v1/documents",
            body = """{"title":"Test Document","content":"Hello World","tags":[]}""",
            bearerToken = accessToken,
        )
        assertEquals(200, createResponse.statusCode())
        val documentId = extractJsonValue(createResponse.body(), "id")
        assertTrue(documentId.isNotBlank())

        // GET (should populate cache)
        val getResponse = request(
            method = "GET",
            path = "/api/v1/documents/$documentId",
            bearerToken = accessToken,
        )
        assertEquals(200, getResponse.statusCode())
        assertTrue(getResponse.body().contains("Test Document"))

        // UPDATE
        val updateResponse = request(
            method = "PUT",
            path = "/api/v1/documents/$documentId",
            body = """{"title":"Updated Title","content":"Updated Content"}""",
            bearerToken = accessToken,
        )
        assertEquals(200, updateResponse.statusCode())
        assertTrue(updateResponse.body().contains("Updated Title"))

        // GET after update (cache should be evicted, returns fresh data)
        val getAfterUpdateResponse = request(
            method = "GET",
            path = "/api/v1/documents/$documentId",
            bearerToken = accessToken,
        )
        assertEquals(200, getAfterUpdateResponse.statusCode())
        assertTrue(getAfterUpdateResponse.body().contains("Updated Title"))

        // DELETE
        val deleteResponse = request(
            method = "DELETE",
            path = "/api/v1/documents/$documentId",
            bearerToken = accessToken,
        )
        assertEquals(204, deleteResponse.statusCode())

        // GET after delete should return 404
        val getAfterDeleteResponse = request(
            method = "GET",
            path = "/api/v1/documents/$documentId",
            bearerToken = accessToken,
        )
        assertEquals(404, getAfterDeleteResponse.statusCode())
    }

    @Test
    fun `trash and restore lifecycle`() {
        val createResponse = request(
            method = "POST",
            path = "/api/v1/documents",
            body = """{"title":"Trash Test","content":"Will be deleted","tags":[]}""",
            bearerToken = accessToken,
        )
        assertEquals(200, createResponse.statusCode())
        val documentId = extractJsonValue(createResponse.body(), "id")

        // DELETE
        request(method = "DELETE", path = "/api/v1/documents/$documentId", bearerToken = accessToken)

        // TRASH list
        val trashResponse = request(
            method = "GET",
            path = "/api/v1/documents/trash",
            bearerToken = accessToken,
        )
        assertEquals(200, trashResponse.statusCode())
        assertTrue(trashResponse.body().contains("Trash Test"))

        // RESTORE
        val restoreResponse = request(
            method = "POST",
            path = "/api/v1/documents/$documentId/restore",
            bearerToken = accessToken,
        )
        assertEquals(200, restoreResponse.statusCode())
        assertTrue(restoreResponse.body().contains("PENDING"))

        // GET after restore
        val getAfterRestoreResponse = request(
            method = "GET",
            path = "/api/v1/documents/$documentId",
            bearerToken = accessToken,
        )
        assertEquals(200, getAfterRestoreResponse.statusCode())
        assertTrue(getAfterRestoreResponse.body().contains("Trash Test"))
    }

    @Test
    fun `revisions are created on update`() {
        val createResponse = request(
            method = "POST",
            path = "/api/v1/documents",
            body = """{"title":"Revision Test","content":"v1","tags":[]}""",
            bearerToken = accessToken,
        )
        val documentId = extractJsonValue(createResponse.body(), "id")

        request(
            method = "PUT",
            path = "/api/v1/documents/$documentId",
            body = """{"title":"Revision Test v2","content":"v2"}""",
            bearerToken = accessToken,
        )

        val revisionsResponse = request(
            method = "GET",
            path = "/api/v1/documents/$documentId/revisions",
            bearerToken = accessToken,
        )
        assertEquals(200, revisionsResponse.statusCode())
        val totalElements = extractJsonValue(revisionsResponse.body(), "totalElements")
        assertTrue(totalElements.toInt() >= 2, "Expected at least 2 revisions, got $totalElements")
    }

    @Test
    fun `cache eviction on write operations`() {
        // Create a document to populate caches
        val createResponse = request(
            method = "POST",
            path = "/api/v1/documents",
            body = """{"title":"Cache Test","content":"Testing cache","tags":[]}""",
            bearerToken = accessToken,
        )
        val documentId = extractJsonValue(createResponse.body(), "id")

        // GET to populate document cache
        request(method = "GET", path = "/api/v1/documents/$documentId", bearerToken = accessToken)

        // Verify cache is populated
        val documentCache = cacheManager.getCache("documents")
        assertTrue(documentCache?.get(documentId.toLong()) != null, "Document should be cached after GET")

        // GET list to populate list cache
        request(method = "GET", path = "/api/v1/documents", bearerToken = accessToken)

        // UPDATE should evict caches
        request(
            method = "PUT",
            path = "/api/v1/documents/$documentId",
            body = """{"title":"Cache Test Updated","content":"Updated"}""",
            bearerToken = accessToken,
        )

        // Verify document cache is evicted
        val cachedAfterUpdate = documentCache?.get(documentId.toLong())
        assertTrue(cachedAfterUpdate == null, "Document cache should be evicted after update")
    }

    @Test
    fun `list documents with pagination`() {
        // Create multiple documents
        for (i in 1..3) {
            request(
                method = "POST",
                path = "/api/v1/documents",
                body = """{"title":"List Test $i","content":"Content $i","tags":[]}""",
                bearerToken = accessToken,
            )
        }

        val listResponse = request(
            method = "GET",
            path = "/api/v1/documents?page=0&size=2",
            bearerToken = accessToken,
        )
        assertEquals(200, listResponse.statusCode())
        val body = listResponse.body()
        val size = extractJsonValue(body, "size")
        assertEquals("2", size)
    }

    @Test
    fun `tags endpoint returns empty list for new document`() {
        val createResponse = request(
            method = "POST",
            path = "/api/v1/documents",
            body = """{"title":"Tag Test","content":"No tags","tags":[]}""",
            bearerToken = accessToken,
        )
        val documentId = extractJsonValue(createResponse.body(), "id")

        val tagsResponse = request(
            method = "GET",
            path = "/api/v1/documents/$documentId/tags",
            bearerToken = accessToken,
        )
        assertEquals(200, tagsResponse.statusCode())
        assertEquals("[]", tagsResponse.body().trim())
    }

    @Test
    fun `ai-status endpoint returns empty for new document`() {
        val createResponse = request(
            method = "POST",
            path = "/api/v1/documents",
            body = """{"title":"AI Status Test","content":"Test","tags":[]}""",
            bearerToken = accessToken,
        )
        val documentId = extractJsonValue(createResponse.body(), "id")

        val aiStatusResponse = request(
            method = "GET",
            path = "/api/v1/documents/$documentId/ai-status",
            bearerToken = accessToken,
        )
        assertEquals(200, aiStatusResponse.statusCode())
        assertTrue(aiStatusResponse.body().contains("\"agents\":[]"))
    }

    private fun request(
        method: String,
        path: String,
        body: String? = null,
        bearerToken: String? = null,
    ): HttpResponse<String> {
        val url = "http://localhost:$port$path"
        val builder = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")

        if (!bearerToken.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $bearerToken")
        }

        when (method) {
            "GET" -> builder.GET()
            "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body ?: "{}"))
            "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(body ?: "{}"))
            "DELETE" -> builder.DELETE()
            else -> error("Unsupported method: $method")
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    private fun extractJsonValue(json: String, field: String): String {
        val regex = Regex("\"$field\"\\s*:\\s*\"?([^,\"\\}]+)\"?")
        return regex.find(json)?.groupValues?.get(1)?.trim() ?: ""
    }
}
