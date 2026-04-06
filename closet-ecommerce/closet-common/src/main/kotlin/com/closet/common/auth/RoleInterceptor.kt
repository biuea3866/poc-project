package com.closet.common.auth

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

private val logger = KotlinLogging.logger {}

/**
 * @RoleRequired 어노테이션 기반 역할 검증 인터셉터.
 *
 * Gateway가 JWT에서 추출한 역할을 X-Member-Role 헤더로 전달하면,
 * 이 인터셉터가 핸들러 메서드의 @RoleRequired와 비교하여 인가를 수행한다.
 *
 * Feature Flag(ROLE_AUTHORIZATION_ENABLED)가 OFF이면 인가 체크를 스킵한다.
 */
@Component
class RoleInterceptor(
    @Value("\${feature-flag.role-authorization-enabled:false}")
    private val roleAuthorizationEnabled: Boolean,
) : HandlerInterceptor {
    companion object {
        const val HEADER_MEMBER_ROLE = "X-Member-Role"
    }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        // Feature Flag OFF -> 인가 체크 스킵
        if (!roleAuthorizationEnabled) {
            return true
        }

        // HandlerMethod가 아닌 경우 (정적 리소스 등) 통과
        if (handler !is HandlerMethod) {
            return true
        }

        // @RoleRequired 어노테이션이 없으면 통과
        val roleRequired = handler.getMethodAnnotation(RoleRequired::class.java) ?: return true

        val requiredRoles = roleRequired.roles.toSet()
        if (requiredRoles.isEmpty()) {
            return true
        }

        // X-Member-Role 헤더에서 역할 추출
        val roleHeader = request.getHeader(HEADER_MEMBER_ROLE)
        val memberRole = MemberRole.fromStringOrDefault(roleHeader)

        // ADMIN은 모든 권한 허용
        if (memberRole == MemberRole.ADMIN) {
            return true
        }

        if (memberRole !in requiredRoles) {
            logger.warn {
                "Role authorization failed: required=$requiredRoles, actual=$memberRole, path=${request.requestURI}"
            }
            throw BusinessException(ErrorCode.FORBIDDEN, "접근 권한이 없습니다. 필요 권한: $requiredRoles")
        }

        return true
    }
}
