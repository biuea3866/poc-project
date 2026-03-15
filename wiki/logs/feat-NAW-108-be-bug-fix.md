# feat/NAW-108-be-bug-fix 작업 로그

---
### 2026-03-15 17:15
- **Agent:** Claude
- **Task:** QA Critical 버그 3건 수정
- **Changes:**
  - wiki-domain/src/main/resources/application.yml (server.port 8081→8080)
  - docker/wiki-api/Dockerfile (EXPOSE 8081→8080)
  - docker-compose.yml (wiki-api 8081:8081→8080:8080, redpanda-console 8080:8080→8180:8080)
  - requirement.md (Auth API에 logout, delete 엔드포인트 추가)
  - wiki/logs/feat-NAW-108-be-bug-fix.md (신규)
- **Decisions:**
  - **버그 1 (403):** 근본 원인은 포트 충돌. QA가 8080으로 요청했으나 8080은 redpanda-console이 점유 중. wiki-api는 8081에서 동작. 포트 통일로 해결.
  - **버그 2 (포트):** wiki-api를 8080(표준)으로 통일. redpanda-console은 8180으로 이동. Dockerfile, docker-compose, application.yml 모두 8080으로 변경.
  - **버그 3 (네이밍):** 실제 구현(`/api/v1/auth/login`)과 requirement.md는 이미 일치. QA의 `/api/v1/auth/sign-in` 사용은 오사용. requirement.md에 미기재된 logout, delete 엔드포인트 추가.
- **Next:** QA 재테스트 후 추가 이슈 확인
---
