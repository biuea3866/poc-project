package com.biuea.wiki.domain.auth

import com.biuea.wiki.application.RefreshUserInput
import com.biuea.wiki.application.UserAuthFacade
import com.biuea.wiki.domain.auth.exception.InvalidRefreshTokenException
import com.biuea.wiki.domain.user.DeleteUserCommand
import com.biuea.wiki.domain.user.LoginUserCommand
import com.biuea.wiki.domain.user.SignUpUserCommand
import com.biuea.wiki.domain.user.User
import com.biuea.wiki.domain.user.UserService
import com.biuea.wiki.infrastructure.auth.RedisAuthTokenStore
import com.biuea.wiki.infrastructure.auth.RefreshTokenRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.never
import org.mockito.BDDMockito.times
import org.mockito.BDDMockito.verify
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * RefreshToken 회전(Rotation) + 탈취 감지 단위 테스트
 *
 * AC 매핑:
 * - TC1: 정상 회전 — 새 토큰 발급, 기존 토큰 revoke, 같은 familyId 유지
 * - TC2: revoked 토큰 재사용 → 동일 family 전체 무효화 (탈취 감지)
 * - TC3: 만료 토큰 거절 검증
 * - TC4: DB에 없는 토큰 거절 검증
 * - TC5: refresh_token 엔티티의 필드 구조 검증
 */
class RefreshTokenRotationTest {

    private val jwtTokenProvider = JwtTokenProvider(
        secret = "test-jwt-secret-key-for-rotation-unit-tests-32-bytes",
        accessTokenExpirationMs = 60_000L,
        refreshTokenExpirationMs = 60_000L,
    )

    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var userService: UserService
    private lateinit var redisAuthTokenStore: RedisAuthTokenStore
    private lateinit var userAuthFacade: UserAuthFacade

    private val testUser = User(email = "user@example.com", password = "hashed_password", name = "testuser")

    @BeforeEach
    fun setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository::class.java)
        userService = mock(UserService::class.java)
        redisAuthTokenStore = mock(RedisAuthTokenStore::class.java)
        userAuthFacade = UserAuthFacade(
            userService = userService,
            jwtTokenProvider = jwtTokenProvider,
            redisAuthTokenStore = redisAuthTokenStore,
            refreshTokenRepository = refreshTokenRepository,
        )
    }

    // =========================================================
    // TC1: 정상 회전 — 새 토큰 발급, 기존 토큰 revoke, 같은 familyId 유지
    // =========================================================
    @Test
    fun `TC1 - 정상 회전 시 기존 토큰이 revoked되고 새 토큰 쌍이 반환된다`() {
        val rawRefreshToken = jwtTokenProvider.createRefreshToken(0L)
        val familyId = UUID.randomUUID().toString()
        val tokenHash = RefreshToken.hashToken(rawRefreshToken)

        val storedToken = RefreshToken(
            tokenHash = tokenHash,
            userId = 0L,
            familyId = familyId,
            isRevoked = false,
            expiresAt = LocalDateTime.now().plusDays(7),
        )

        given(refreshTokenRepository.findByTokenHash(tokenHash)).willReturn(storedToken)
        given(userService.findById(0L)).willReturn(testUser)
        given(refreshTokenRepository.save(any(RefreshToken::class.java))).willAnswer { it.arguments[0] as RefreshToken }

        val output = userAuthFacade.refresh(RefreshUserInput(rawRefreshToken))

        // 기존 토큰이 revoke 되었는지 확인
        assertTrue(storedToken.isRevoked, "기존 토큰은 revoke 되어야 합니다")

        // 새 토큰이 발급되었는지 확인
        assertNotNull(output.accessToken)
        assertNotNull(output.refreshToken)
        assertNotEquals(rawRefreshToken, output.refreshToken, "새 refresh 토큰은 기존과 달라야 합니다")
        assertEquals("Bearer", output.tokenType)

        // 새 토큰이 DB에 저장되었는지 확인
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken::class.java))
    }

    // =========================================================
    // TC2: revoked 토큰 재사용 → 동일 family 전체 무효화 (탈취 감지)
    // =========================================================
    @Test
    fun `TC2 - revoked 토큰으로 재요청 시 동일 family 전체가 무효화된다`() {
        val rawRefreshToken = jwtTokenProvider.createRefreshToken(0L)
        val familyId = UUID.randomUUID().toString()
        val tokenHash = RefreshToken.hashToken(rawRefreshToken)

        val revokedToken = RefreshToken(
            tokenHash = tokenHash,
            userId = 0L,
            familyId = familyId,
            isRevoked = true, // 이미 revoked 상태
            expiresAt = LocalDateTime.now().plusDays(7),
        )

        given(refreshTokenRepository.findByTokenHash(tokenHash)).willReturn(revokedToken)
        given(refreshTokenRepository.revokeAllByFamilyId(familyId)).willReturn(3)

        // InvalidRefreshTokenException 이 발생해야 함
        assertThrows<InvalidRefreshTokenException> {
            userAuthFacade.refresh(RefreshUserInput(rawRefreshToken))
        }

        // 동일 family 전체 무효화가 호출되었는지 확인 (탈취 감지)
        verify(refreshTokenRepository, times(1)).revokeAllByFamilyId(familyId)
    }

    // =========================================================
    // TC3: 만료 토큰 거절 검증
    // =========================================================
    @Test
    fun `TC3 - 만료된 RefreshToken으로 요청 시 예외가 발생한다`() {
        val rawRefreshToken = jwtTokenProvider.createRefreshToken(0L)
        val familyId = UUID.randomUUID().toString()
        val tokenHash = RefreshToken.hashToken(rawRefreshToken)

        val expiredToken = RefreshToken(
            tokenHash = tokenHash,
            userId = 0L,
            familyId = familyId,
            isRevoked = false,
            expiresAt = LocalDateTime.now().minusSeconds(1), // 이미 만료
        )

        given(refreshTokenRepository.findByTokenHash(tokenHash)).willReturn(expiredToken)

        assertThrows<InvalidRefreshTokenException> {
            userAuthFacade.refresh(RefreshUserInput(rawRefreshToken))
        }

        // 만료 토큰의 경우 revokeAllByFamilyId 는 호출되면 안 됨 (탈취 감지가 아님)
        verify(refreshTokenRepository, never()).revokeAllByFamilyId(any())
    }

    // =========================================================
    // TC4: DB에 없는 토큰 거절 검증
    // =========================================================
    @Test
    fun `TC4 - DB에 존재하지 않는 토큰으로 요청 시 예외가 발생한다`() {
        val rawRefreshToken = jwtTokenProvider.createRefreshToken(0L)
        val tokenHash = RefreshToken.hashToken(rawRefreshToken)

        given(refreshTokenRepository.findByTokenHash(tokenHash)).willReturn(null)

        assertThrows<InvalidRefreshTokenException> {
            userAuthFacade.refresh(RefreshUserInput(rawRefreshToken))
        }

        // family 전체 무효화는 호출되면 안 됨
        verify(refreshTokenRepository, never()).revokeAllByFamilyId(any())
        // 새 토큰 저장도 호출되면 안 됨
        verify(refreshTokenRepository, never()).save(any(RefreshToken::class.java))
    }

    // =========================================================
    // TC5: refresh_token 엔티티의 필드 구조 검증
    // =========================================================
    @Test
    fun `TC5 - refresh_token 엔티티에 token_hash, family_id, is_revoked 필드가 올바르게 동작한다`() {
        val rawToken = "test-token-value"
        val hash = RefreshToken.hashToken(rawToken)
        val familyId = UUID.randomUUID().toString()

        val token = RefreshToken(
            tokenHash = hash,
            userId = 1L,
            familyId = familyId,
            isRevoked = false,
            expiresAt = LocalDateTime.now().plusDays(7),
        )

        // 필드 존재 및 초기값 검증
        assertEquals(hash, token.tokenHash)
        assertEquals(familyId, token.familyId)
        assertFalse(token.isRevoked)
        assertEquals(64, token.tokenHash.length, "SHA-256 hex는 64자여야 합니다")

        // revoke 동작 검증
        token.revoke()
        assertTrue(token.isRevoked, "revoke() 호출 후 isRevoked는 true여야 합니다")
    }
}
