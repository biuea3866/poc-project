---
name: pr-review-checklist
description: PR 리뷰 시 점검할 항목 체크리스트 — 하네스 룰, 레이어 경계, 트랜잭션 위치, 테스트 커버리지, 보안, 성능, 가독성. pr-reviewer 에이전트가 이 절차로 검수 후 verdict를 낸다.
---

# PR Review Checklist Skill

## 언제 사용하나
- PR URL/번호를 받았을 때
- "리뷰해줘", "머지해도 되나?" 요청

## 원칙
- 모든 지적은 **파일:라인 + 근거 + 제안 수정**
- 스타일만 지적하는 리뷰 금지 — 아키텍처/안전성 우선
- Verdict는 객관적 기준 충족 여부로 결정

## 체크리스트

### A. 변경 범위 파악
- [ ] `gh pr diff`로 파일 단위 스캔
- [ ] 대응 Jira 티켓 / Acceptance Criteria 확인
- [ ] 커밋 메시지가 변경 범위와 일치

### B. 하네스 룰 (Critical 분류)
- [ ] `@Query` 사용 없음 (QueryDSL 사용)
- [ ] `LocalDateTime` 사용 없음 (ZonedDateTime)
- [ ] `ConsumerRecord<String, String>` 없음 (DTO 직접 매핑)
- [ ] Consumer에서 Repository 직접 호출 없음
- [ ] SQL: FK/JSON/ENUM/BOOLEAN 없음, `DATETIME(6)` 사용
- [ ] Kafka 토픽 하드코딩 없음 (상수 클래스 사용)

### C. 아키텍처/레이어 (Major)
- [ ] Controller → Facade → Service → Repository 경로 준수
- [ ] UseCase가 Repository 직접 호출 안 함 (DomainService 경유)
- [ ] BFF Controller가 Client 직접 호출 안 함 (Facade 경유)
- [ ] Entity에 비즈니스 로직 캡슐화, Service는 얇음
- [ ] `@Transactional`이 UseCase/DomainService에만

### D. 테스트 (Major)
- [ ] 변경 코드에 대응하는 테스트 존재
- [ ] 통합 테스트(Testcontainers) 포함
- [ ] Given/When/Then 구조
- [ ] 실패 경로 테스트 ≥1개
- [ ] 커버리지 목표 충족 (일반 80% / 핵심 95%)

### E. 보안 (Critical)
- [ ] 입력 검증 (Bean Validation 또는 수동)
- [ ] 인증/인가 (SecurityContext, 권한 체크)
- [ ] 민감정보 로깅 없음 (PII, 토큰, 비밀번호)
- [ ] SQL Injection 방지 (파라미터 바인딩)

### F. 성능 (Major)
- [ ] N+1 쿼리 없음 (fetch join, @EntityGraph, QueryDSL)
- [ ] 불필요한 fetch 없음 (필요한 컬럼만)
- [ ] 락 순서/범위 적절
- [ ] 페이지네이션 (대량 조회 시)

### G. 가독성 (Minor)
- [ ] 네이밍 풀네임 (`workspaceId` ✓, `ws` ✗)
- [ ] FQCN 미사용 (import 사용)
- [ ] 불필요한 주석/죽은 코드 없음
- [ ] 파일 크기/메서드 길이 합리적

## Verdict 기준
| 상태 | 조건 |
|---|---|
| `approve` | Critical 0 + Major 0 |
| `comment` | Major 1-2, 수정 권장 (선택) |
| `request-changes` | Critical ≥1 또는 Major ≥3 |

## 운영 규칙
- **FE/DevOps PR**: 자동 approve + 머지 가능 (단 테스트/하네스 통과 시)
- **BE PR**: 자동 approve 금지 — 사람 리뷰 대기 상태로 comment만 남김

## 출력 형식
```
gh pr review <num> --body "$(cat <<'EOF'
## Summary
<요약 2-3문장>

## Findings
### Critical
- <파일:라인> <지적>
  제안: <수정 방향>

### Major
- ...

### Minor
- ...

## Verdict: <approve/comment/request-changes>
EOF
)"
```