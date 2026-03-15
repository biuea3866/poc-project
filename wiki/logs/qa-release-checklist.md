# QA Release Checklist
**Project:** AI Wiki (NAW)
**Last Updated:** 2026-03-15
**Author:** QA Engineer (Claude)

> development → main 릴리즈 PR 본문에 포함할 체크리스트.
> 모든 항목이 통과해야 릴리즈 승인.

---

## 1. Auth API
- [ ] `POST /api/v1/auth/signup` — 회원가입 (200, 사용자 정보 반환)
- [ ] `POST /api/v1/auth/signup` — 중복 이메일 시 적절한 에러 반환
- [ ] `POST /api/v1/auth/login` — 로그인 (200, accessToken + refreshToken 반환)
- [ ] `POST /api/v1/auth/login` — 잘못된 비밀번호 시 401 반환
- [ ] `POST /api/v1/auth/refresh` — 토큰 갱신 (200, 새 accessToken 반환)
- [ ] `POST /api/v1/auth/refresh` — 만료된 refreshToken 시 적절한 에러 반환
- [ ] `POST /api/v1/auth/logout` — 로그아웃 (204)
- [ ] 미인증 요청 시 401 반환
- [ ] 인증 + 미존재 엔드포인트 시 404 반환

## 2. Document CRUD
- [ ] `POST /api/v1/documents` — 문서 생성 (200, id/title/content/status 반환)
- [ ] `GET /api/v1/documents` — 문서 목록 조회 (200, 페이지네이션: page/size/totalElements/totalPages)
- [ ] `GET /api/v1/documents/{id}` — 문서 상세 조회 (200)
- [ ] `PUT /api/v1/documents/{id}` — 문서 수정 (200)
- [ ] `DELETE /api/v1/documents/{id}` — 소프트 삭제 (204, status→DELETED)
- [ ] `GET /api/v1/documents/trash` — 휴지통 목록 조회 (200, 삭제된 문서만)
- [ ] `POST /api/v1/documents/{id}/restore` — 문서 복구 (200, status→PENDING)
- [ ] 페이지네이션 파라미터 동작 확인 (page, size)

## 3. Document 발행
- [ ] `POST /api/v1/documents/{id}/publish` — 문서 발행 (PENDING → ACTIVE)
- [ ] 발행 시 Kafka 이벤트 발행 확인

## 4. AI 상태
- [ ] `GET /api/v1/documents/{id}/ai-status` — AI 처리 상태 조회 (200)
- [ ] 발행 후 AI 상태 전환: PENDING → PROCESSING → COMPLETED

## 5. Search
- [ ] `GET /api/v1/search/integrated?q={keyword}` — 통합 검색 (200, 결과 목록 + 페이지네이션)
- [ ] 검색 결과에 title, snippet 포함 확인
- [ ] AI 벡터 검색 동작 확인 (semantic 검색 결과 반환)

## 6. FE 스모크 테스트
- [ ] `GET /` — 메인 페이지 (200)
- [ ] `GET /login` — 로그인 페이지 (200)
- [ ] `GET /signup` — 회원가입 페이지 (200)
- [ ] `GET /dashboard` — 대시보드 (200)
- [ ] `GET /search` — 검색 페이지 (200)
- [ ] 브라우저 콘솔 에러 없음

## 7. API 응답 시간
- [ ] `POST /api/v1/auth/login` — p95 < 500ms
- [ ] `GET /api/v1/documents` — p95 < 500ms
- [ ] `GET /api/v1/search/integrated` — p95 < 500ms
- [ ] `POST /api/v1/documents` — p95 < 500ms

## 8. CORS
- [ ] 브라우저(localhost:3000)에서 BE API(localhost:8080) 호출 시 CORS 에러 없음
- [ ] Preflight (OPTIONS) 요청 정상 처리
- [ ] 인증 헤더(Authorization: Bearer) 포함 요청 정상

## 9. 인프라
- [ ] MySQL (3306) — 정상 기동
- [ ] Redis (6379) — 정상 기동
- [ ] Kafka (9094) — 정상 기동
- [ ] BE API (8080) — Health Check UP
- [ ] FE (3000) — 정상 기동

---

## 릴리즈 판정 기준
- **READY:** 모든 항목 체크 완료
- **NOT READY:** 1개 이상 실패 시 → 실패 항목 + 원인 + 담당자 기록 후 재테스트
