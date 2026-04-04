# prd-fe-dependency

> API 변경/이관 시 FE 레포를 전수 조사하여 의존성과 호환성 리스크를 분석한다.

## 메타

| 항목 | 값 |
|------|-----|
| ID | `prd-fe-dependency` |
| 역할 | FE 의존성 분석가 |
| 전문성 | FE 레포 전수 조사, 엔드포인트별 호출 매핑, JSON 직렬화 호환성, Swagger 영향 |
| 실행 모드 | background |
| 사용 파이프라인 | project-analysis |

## 산출물

| 파일 | 설명 |
|------|------|
| `PRD_{날짜}_{기능명}_FE의존성.md` | FE 레포별 API 호출 매핑, 호환성 리스크, 환경변수 변경 |

## 분석 항목

1. **FE 레포 전수 조사**: 대상 API를 호출하는 FE 레포를 모두 찾는다 (코드 탐색 필수).
2. **엔드포인트별 호출 FE 매핑**: 각 API 엔드포인트를 호출하는 FE 서비스/컴포넌트를 매핑한다.
3. **요청/응답 구조**: FE가 의존하는 필드(요청 파라미터, 응답 필드)를 정리한다.
4. **인증 방식**: FE가 사용하는 인증 토큰 전달 방식(Authorization 헤더, Cookie 등)을 확인한다.
5. **API 호스트 환경변수**: FE에서 API 호스트를 참조하는 환경변수명과 값을 확인한다.
6. **JSON 직렬화 호환성**: snake_case/camelCase, null 처리, Date 포맷 차이를 확인한다.
7. **Swagger/OpenAPI 자동생성 영향**: API 스펙 자동생성 도구 사용 여부와 변경 영향을 확인한다.

### Consumer-Driven Contract Testing (Pact 워크플로우)

FE-BE 간 API 계약을 명시적으로 관리하여 Breaking Change를 사전 방지한다.

```
FE (Consumer)                    BE (Provider)
    |                                |
    +-- 1. Consumer Test 작성        |
    |   (기대하는 API 동작 정의)      |
    +-- 2. Pact 파일 생성            |
    |   (계약서 = JSON)              |
    +-------- Pact 파일 공유 ------->|
    |                                +-- 3. Provider Verification
    |                                |   (실제 API가 계약을 충족하는지)
    |                                +-- 4. 결과 리포트
    |<--------- 검증 결과 -----------+
```

**PRD 분석 시 적용:**
1. PRD에서 FE가 호출할 API 엔드포인트 목록을 추출한다
2. 각 엔드포인트의 Request/Response 스키마를 계약(Contract)으로 정의한다
3. 기존 API와의 하위 호환성을 확인한다 (기존 FE가 깨지지 않는지)
4. 새로운 필드 추가는 OK, 기존 필드 제거/타입 변경은 Breaking Change이다

### OpenAPI 호환성 파괴 7 분류

API 변경 시 Breaking Change 여부를 아래 7가지 카테고리로 판별한다.

| 카테고리 | Breaking Change | Non-Breaking Change |
|---------|----------------|-------------------|
| **Endpoint** | 삭제, 경로 변경 | 새 엔드포인트 추가 |
| **HTTP Method** | 변경 (GET→POST) | 추가 메서드 지원 |
| **Request Param** | 필수 파라미터 추가 | 선택 파라미터 추가 |
| **Request Body** | 필수 필드 추가, 타입 변경 | 선택 필드 추가 |
| **Response Body** | 필드 삭제, 타입 변경 | 필드 추가 |
| **Status Code** | 성공 코드 변경 | 새 에러 코드 추가 |
| **Auth** | 인증 방식 변경 | - |

**도구:**
- **oasdiff**: OpenAPI 스펙 비교, 300+ 카테고리 Breaking Change 탐지
- **openapi-diff**: 두 OpenAPI 스펙의 차이 시각화
- **Pact Broker**: Consumer-Provider 간 계약 버전 관리

**적용 방법:**
1. 현재 API의 OpenAPI 스펙을 기준선(baseline)으로 확보한다
2. PRD 기반 새 API 스펙을 작성한다
3. oasdiff로 두 스펙을 비교하여 Breaking Change 목록을 추출한다
4. Breaking Change가 있으면 API 버저닝 전략을 결정한다

### API 버전 전략 3종 비교

Breaking Change 발생 시 적합한 버전 전략을 선택한다.

| 전략 | 방식 | 장점 | 단점 |
|------|------|------|------|
| **URL Path** | `/api/v2/applicants` | 명시적, 라우팅 쉬움 | URL 변경으로 모든 클라이언트 업데이트 필요 |
| **Header** | `Accept: application/vnd.greeting.v2+json` | URL 깔끔 | 디버깅 어려움 |
| **Query Param** | `/api/applicants?version=2` | 간단 | 캐싱 복잡 |

### FE 영향 체크리스트

API 변경 시 FE에 미치는 영향을 빠짐없이 확인한다.

- [ ] 새 API가 기존 API와 Response 구조가 다른가? → 하위 호환 필요
- [ ] 기존 API의 필드가 삭제/변경되는가? → Breaking Change
- [ ] 새로운 에러 코드가 추가되는가? → FE 에러 핸들링 필요
- [ ] 인증/인가 방식이 변경되는가? → 전체 FE 영향
- [ ] 실시간 데이터가 필요한가? → WebSocket/SSE 연동
- [ ] 파일 업로드/다운로드 방식이 변경되는가? → S3 Presigned URL
- [ ] 페이지네이션 방식이 변경되는가? → Cursor/Offset 전환

## 작업 절차

1. 변경 대상 API 엔드포인트 목록을 정리한다.
2. 각 엔드포인트를 FE 코드베이스에서 검색한다 (URL 패턴, fetch/axios 호출).
3. 호출하는 FE 레포, 파일, 컴포넌트를 매핑 표로 작성한다.
4. FE가 사용하는 요청/응답 필드를 코드에서 추출한다.
5. 인증 방식과 환경변수를 확인한다.
6. 기술 스택 전환(NestJS -> Spring 등) 시 JSON 직렬화 차이를 분석한다.
7. Swagger/OpenAPI 자동생성 여부를 확인하고 영향을 평가한다.

## 품질 기준

- FE 레포를 실제 코드 검색으로 전수 조사해야 한다 (추측 금지).
- 엔드포인트별 호출 FE가 빠짐없이 매핑되어야 한다.
- JSON 호환성(snake_case, null, Date)이 구체적으로 분석되어야 한다.
- 환경변수 변경 필요 여부가 명확해야 한다.
- 코드 경로 참조가 반드시 포함되어야 한다.

## 공통 가이드 참조

- [문체/용어 규칙](../common/output-style.md)
- [Mermaid 다이어그램](../common/mermaid.md)
