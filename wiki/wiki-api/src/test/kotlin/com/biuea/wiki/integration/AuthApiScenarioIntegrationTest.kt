package com.biuea.wiki.integration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID

class AuthApiScenarioIntegrationTest(
    @Value("\${local.server.port}") private val port: Int,
) : BaseIntegrationTest() {
    private val httpClient: HttpClient = HttpClient.newHttpClient()

    @Test
    fun `auth endpoint scenario works end-to-end`() {
        val email = "user-${UUID.randomUUID()}@example.com"
        val password = "password123!"
        val name = "integration-user"

        val signUpResponse = request(
            method = "POST",
            path = "/api/v1/auth/signup",
            body = """{"email":"$email","password":"$password","name":"$name"}""",
        )
        assertEquals(200, signUpResponse.statusCode())

        val duplicateSignUpResponse = request(
            method = "POST",
            path = "/api/v1/auth/signup",
            body = """{"email":"$email","password":"$password","name":"dup"}""",
        )
        assertEquals(409, duplicateSignUpResponse.statusCode())

        val invalidLoginResponse = request(
            method = "POST",
            path = "/api/v1/auth/login",
            body = """{"email":"$email","password":"wrong-password"}""",
        )
        assertEquals(401, invalidLoginResponse.statusCode())

        val loginResponse = request(
            method = "POST",
            path = "/api/v1/auth/login",
            body = """{"email":"$email","password":"$password"}""",
        )
        assertEquals(200, loginResponse.statusCode())
        val accessToken = extractJsonValue(loginResponse.body(), "accessToken")
        val refreshToken = extractJsonValue(loginResponse.body(), "refreshToken")
        assertTrue(accessToken.isNotBlank())
        assertTrue(refreshToken.isNotBlank())

        val invalidRefreshResponse = request(
            method = "POST",
            path = "/api/v1/auth/refresh",
            body = """{"refreshToken":"invalid-refresh-token"}""",
        )
        assertEquals(401, invalidRefreshResponse.statusCode())

        val refreshResponse = request(
            method = "POST",
            path = "/api/v1/auth/refresh",
            body = """{"refreshToken":"$refreshToken"}""",
        )
        assertEquals(200, refreshResponse.statusCode())
        val refreshedAccessToken = extractJsonValue(refreshResponse.body(), "accessToken")
        assertTrue(refreshedAccessToken.isNotBlank())

        val unauthenticatedDeleteResponse = request(
            method = "DELETE",
            path = "/api/v1/auth/me",
        )
        assertUnauthorizedOrForbidden(unauthenticatedDeleteResponse.statusCode())

        val logoutResponse = request(
            method = "POST",
            path = "/api/v1/auth/logout",
            body = """{"refreshToken":"$refreshToken"}""",
            bearerToken = accessToken,
        )
        assertEquals(204, logoutResponse.statusCode())

        val refreshAfterLogoutResponse = request(
            method = "POST",
            path = "/api/v1/auth/refresh",
            body = """{"refreshToken":"$refreshToken"}""",
        )
        assertEquals(401, refreshAfterLogoutResponse.statusCode())

        val blacklistedAccessResponse = request(
            method = "POST",
            path = "/api/v1/auth/logout",
            body = """{"refreshToken":"$refreshToken"}""",
            bearerToken = accessToken,
        )
        assertUnauthorizedOrForbidden(blacklistedAccessResponse.statusCode())

        val deleteUserEmail = "delete-${UUID.randomUUID()}@example.com"
        val deleteUserPassword = "password456!"

        val deleteUserSignUpResponse = request(
            method = "POST",
            path = "/api/v1/auth/signup",
            body = """{"email":"$deleteUserEmail","password":"$deleteUserPassword","name":"delete-user"}""",
        )
        assertEquals(200, deleteUserSignUpResponse.statusCode())

        val reloginResponse = request(
            method = "POST",
            path = "/api/v1/auth/login",
            body = """{"email":"$deleteUserEmail","password":"$deleteUserPassword"}""",
        )
        assertEquals(200, reloginResponse.statusCode())
        val secondAccessToken = extractJsonValue(reloginResponse.body(), "accessToken")
        assertTrue(secondAccessToken.isNotBlank())

        val deleteMeResponse = request(
            method = "DELETE",
            path = "/api/v1/auth/me",
            bearerToken = secondAccessToken,
        )
        assertEquals(204, deleteMeResponse.statusCode())

        val loginAfterDeleteResponse = request(
            method = "POST",
            path = "/api/v1/auth/login",
            body = """{"email":"$deleteUserEmail","password":"$deleteUserPassword"}""",
        )
        assertEquals(401, loginAfterDeleteResponse.statusCode())
    }

    @Test
    fun `GET tag types returns all tag constants`() {
        val response = request(method = "GET", path = "/api/v1/tags/types")
        assertEquals(200, response.statusCode())
        val body = response.body()
        assertTrue(body.contains("TECH"))
        assertTrue(body.contains("BACKEND"))
        assertTrue(body.contains("FRONTEND"))
        assertTrue(body.contains("AI"))
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
            "DELETE" -> builder.DELETE()
            else -> error("Unsupported method: $method")
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    private fun extractJsonValue(json: String, field: String): String {
        val regex = Regex("\"$field\"\\s*:\\s*\"([^\"]+)\"")
        return regex.find(json)?.groupValues?.get(1) ?: ""
    }

    private fun assertUnauthorizedOrForbidden(statusCode: Int) {
        assertTrue(statusCode == 401 || statusCode == 403, "Expected 401/403 but was $statusCode")
    }
}
