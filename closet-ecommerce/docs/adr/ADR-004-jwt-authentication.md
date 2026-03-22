# ADR-004: JWT 인증 전략

## 상태: 승인

## 컨텍스트
이커머스 서비스의 사용자 인증/인가 메커니즘을 결정해야 한다.
- 상태 비저장(Stateless) 인증 선호
- 토큰 탈취 시 대응 방안 필요
- 모바일/웹 클라이언트 모두 지원
- 향후 MSA 전환 시에도 호환 가능해야 함

## 결정
JWT 기반 Access Token + Refresh Token 이중 토큰 전략을 채택한다.
- **Access Token**: 30분 만료, 요청마다 전송
- **Refresh Token**: 7일 만료, HttpOnly 쿠키 또는 별도 저장
- **Redis 블랙리스트**: 로그아웃/강제 만료 시 Access Token의 남은 TTL만큼 블랙리스트에 등록
- **토큰 재발급**: Refresh Token으로 `/api/v1/member/auth/reissue` 호출

## 이유
- Access Token 30분: 보안(짧은 유효기간)과 UX(잦은 재로그인 방지)의 균형
- Refresh Token 7일: 사용자 편의성 확보
- Redis 블랙리스트: JWT의 Stateless 특성의 한계(즉시 무효화 불가)를 보완
- 이중 토큰: Access Token 탈취 시 피해 범위를 30분으로 제한

## 결과
- 모든 인증 필요 API는 `Authorization: Bearer {accessToken}` 헤더 필수
- JwtFilter에서 토큰 검증 + 블랙리스트 체크
- Refresh Token은 DB에 저장하여 1:1 매핑 (다중 기기 지원 시 변경)
- Redis Key: `blacklist:{tokenId}`, TTL: 토큰 잔여 만료 시간
- 토큰 페이로드: `{ sub: memberId, role: ROLE_USER, iat, exp }`

## 대안 (검토했으나 선택하지 않은 것)

### 세션 기반 인증
- 장점: 즉시 무효화 가능, 구현 단순
- 단점: 서버 상태 유지 필요, 수평 확장 시 세션 클러스터링 필요
- 기각 사유: MSA 전환 시 서비스 간 세션 공유 문제

### OAuth 2.0 (소셜 로그인만)
- 장점: 자체 인증 로직 불필요
- 단점: 자체 회원 체계와 통합 복잡, 소셜 의존성
- 기각 사유: 이커머스에서 자체 회원 관리는 필수 (향후 소셜 로그인은 추가 가능)

### Access Token만 사용 (장기 유효)
- 장점: 구현 단순
- 단점: 토큰 탈취 시 장기간 악용 가능, 블랙리스트 부담 증가
- 기각 사유: 보안 리스크가 과도
