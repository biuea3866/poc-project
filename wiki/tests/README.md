# Tests

QA 릴리즈 게이트 테스트 스위트

## 구성

```
tests/
├── k6/                         # k6 부하 테스트
│   ├── config.js               # 공통 설정 및 헬퍼
│   ├── load-test.js            # 전체 API 부하 테스트
│   ├── run-all.sh              # 전체 k6 테스트 실행 스크립트
│   ├── scenarios/
│   │   ├── 01-auth.js          # 인증 시나리오 (VU 10, 30s)
│   │   ├── 02-documents.js     # 문서 CRUD 시나리오 (VU 20, 60s)
│   │   ├── 03-search.js        # 검색 시나리오 (VU 10, 30s)
│   │   └── 04-full-user-journey.js  # 전체 사용자 여정 (VU 30, ramp)
│   └── results/                # 테스트 결과 JSON (gitignore)
└── e2e/                        # Playwright E2E 테스트
    ├── playwright.config.ts    # Playwright 설정
    ├── package.json
    ├── helpers/
    │   └── auth.ts             # 인증 헬퍼 (createTestUser, loginUser)
    ├── specs/
    │   ├── 01-css-check.spec.ts     # CSS/레이아웃 체크
    │   ├── 02-auth.spec.ts          # 인증 플로우
    │   ├── 03-documents.spec.ts     # 문서 CRUD
    │   └── 04-search.spec.ts        # 검색
    └── screenshots/            # 스크린샷 저장 디렉토리
```

---

## 사전 요구사항

서비스가 실행 중이어야 합니다:
- **wiki-api**: `http://localhost:8081`
- **frontend**: `http://localhost:3000`

```bash
# 전체 서비스 실행 (개발 환경)
cd /path/to/wiki
docker compose -f docker-compose.yml -f docker-compose.development.yml up -d

# frontend 실행
cd frontend && npm run dev
```

---

## k6 테스트

### k6 설치

```bash
# macOS
brew install k6

# Ubuntu/Debian
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6

# Windows (Chocolatey)
choco install k6
```

### 로컬 실행

```bash
# 전체 시나리오 실행 (결과 JSON 저장)
cd tests/k6
./run-all.sh

# 개별 시나리오 실행
k6 run --env API_BASE=http://localhost:8081 scenarios/01-auth.js
k6 run --env API_BASE=http://localhost:8081 scenarios/02-documents.js
k6 run --env API_BASE=http://localhost:8081 scenarios/03-search.js
k6 run --env API_BASE=http://localhost:8081 scenarios/04-full-user-journey.js

# 부하 테스트 실행
k6 run --env API_BASE=http://localhost:8081 load-test.js
```

### k6 임계값 (Thresholds)

| 메트릭 | 기준 |
|--------|------|
| `http_req_failed` | `< 5%` (HTTP 에러율) |
| `http_req_duration p(95)` | `< 2000ms` |
| `http_req_duration p(99)` | `< 5000ms` (부하 테스트) |
| `auth_success_rate` | `>= 95%` |
| `new_user_journey_success` | `>= 90%` |
| `existing_user_journey_success` | `>= 90%` |

---

## Playwright E2E 테스트

### Playwright 설치

```bash
cd tests/e2e

# 의존성 설치
npm install

# Chromium 브라우저 설치
npx playwright install chromium
# 또는 시스템 의존성 포함
npx playwright install chromium --with-deps
```

### 로컬 실행

```bash
cd tests/e2e

# 전체 테스트 실행
npm test

# 개별 스펙 실행
npm run test:css        # CSS/레이아웃 체크
npm run test:auth       # 인증 플로우
npm run test:documents  # 문서 CRUD
npm run test:search     # 검색

# 헤드 모드로 실행 (브라우저 창 표시)
npm run test:headed

# 디버그 모드
npm run test:debug

# HTML 리포트 보기
npm run report
```

### 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `BASE_URL` | `http://localhost:3000` | 프론트엔드 URL |
| `API_BASE` | `http://localhost:8081` | 백엔드 API URL |
| `CI` | (없음) | CI 환경 여부 (headless 강제) |

---

## GitHub Actions QA 게이트

`.github/workflows/qa-release-gate.yml` 에 정의된 워크플로우:

1. **build-check** — Kotlin 컴파일 + Next.js 빌드
2. **backend-unit-tests** — `./gradlew test`
3. **e2e-tests** — Playwright E2E 테스트 (build-check 완료 후)
4. **k6-load-tests** — k6 부하 테스트 (e2e-tests 완료 후)
5. **qa-gate** — 모든 job 결과 확인, 실패시 PR 머지 차단

### 트리거

- `development` 브랜치 대상 PR
- `main` 브랜치 대상 PR

### 실패 시

QA 게이트 실패 시 PR에 자동으로 실패 내용이 코멘트되고 머지가 차단됩니다.
Artifacts (스크린샷, 비디오, k6 결과 JSON)를 다운로드해서 원인 분석이 가능합니다.
