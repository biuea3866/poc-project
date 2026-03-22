# PRD 분석 파이프라인

> 신규 피쳐, 기능 변경, 기획서 검토 시 사용

---

## 파이프라인 흐름

```
[INPUT] PRD/기획서/기능 요구사항
    │
    ▼
━━ Phase 1: 요구사항 파싱 ━━━━━━━━━━━━━━━━━━━━━━━━━━━
    │  팀장이 PRD를 읽고 핵심 요구사항 추출
    │  - 기능 목표 정리
    │  - 변경 범위 예측
    │  - 필요 에이전트 수/역할 결정
    │
    ▼
━━ Phase 2: 영향 범위 분석 (병렬) ━━━━━━━━━━━━━━━━━━━━
    │  에이전트들이 동시에 관련 코드 탐색
    │  ┌─ Agent A: FE 영향 분석
    │  ├─ Agent B: BE 영향 분석
    │  ├─ Agent C: DB/인프라 영향 분석
    │  └─ Agent D: 외부 연동 영향 분석
    │
    ▼
━━ Phase 3: 구현 설계 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    │  팀장이 분석 결과를 종합하여 설계 도출
    │  - 수정 대상 파일/함수 목록
    │  - 신규 생성 필요 파일 목록
    │  - API 변경 사항
    │  - DB 마이그레이션 필요 여부
    │
    ▼
━━ Phase 4: 리스크 & 의존성 검토 ━━━━━━━━━━━━━━━━━━━━
    │  - 다른 서비스에 미치는 영향
    │  - 하위 호환성 이슈
    │  - 성능/보안 리스크
    │  - 롤백 전략
    │
    ▼
[OUTPUT] PRD 분석 보고서
```

---

## 에이전트 역할 정의

### 팀장 (메인 Claude)
- PRD 요구사항 파싱 및 분류
- 에이전트 수/역할 동적 결정
- 분석 결과 통합 및 최종 보고서 작성
- 사용자와의 커뮤니케이션

### Agent A: FE 영향 분석가
**트리거 조건**: PRD에 UI 변경, 사용자 흐름 변경, 화면 추가가 포함된 경우

**분석 항목**:
1. 관련 FE 모듈 식별
2. 수정 필요 컴포넌트/페이지 목록
3. 라우팅 변경 필요 여부
4. 상태 관리 변경 필요 여부
5. API 호출 추가/변경 필요 여부
6. 공유 컴포넌트 라이브러리 변경 필요 여부
7. 국제화 키 추가 필요 여부

**탐색 대상**:
```
- 라우팅: src/routes, pages/, app/
- 컴포넌트: src/components/, src/pages/
- API: src/api/, src/apis/
- 상태: src/store/, src/context/
- 타입: src/types/
```

### Agent B: BE 영향 분석가
**트리거 조건**: PRD에 데이터 변경, 비즈니스 로직 변경, API 추가가 포함된 경우

**분석 항목**:
1. 관련 BE 서비스 및 모듈 식별
2. 수정 필요 Controller/Service/Domain 목록
3. 신규 API 엔드포인트 설계
4. 도메인 모델 변경 필요 여부
5. Kafka 이벤트 추가/변경 필요 여부
6. 트랜잭션 경계 검토
7. API Gateway 라우팅 변경 필요 여부

**탐색 대상**:
```
- API: presentation/api/
- 서비스: business/application/
- 도메인: business/domain/
- DB: adaptor/mysql/
- 이벤트: adaptor/kafka/
- 캐시: adaptor/redis/
- 설정: src/main/resources/application*.yml
```

### Agent C: DB/인프라 영향 분석가
**트리거 조건**: PRD에 데이터 모델 변경, 새 엔티티, 검색 기능이 포함된 경우

**분석 항목**:
1. 테이블 스키마 변경 필요 여부
2. Flyway 마이그레이션 스크립트 작성 필요
3. 인덱스 추가 필요 여부
4. Redis 캐시 전략 변경
5. Kafka 토픽 추가
6. Elasticsearch 인덱스 변경

**탐색 대상**:
```
- 스키마: 각 서비스의 src/main/resources/db/migration/
- 엔티티: adaptor/mysql/entity/
- 검색: closet-search/
```

### Agent D: 외부 연동 영향 분석가
**트리거 조건**: PRD에 외부 서비스 연동, 알림 발송, 결제 변경이 포함된 경우

**분석 항목**:
1. 외부 API 호출 추가/변경
2. 알림 채널 (메일/SMS/알림톡/푸시) 변경
3. PG 결제 플로우 변경
4. 택배사 API 연동 변경
5. AWS 서비스 변경 (S3, SES 등)

**탐색 대상**:
```
- 결제: closet-payment/adaptor/pg/
- 배송: closet-shipping/adaptor/delivery/
- 알림: closet-notification/
- 검색: closet-search/adaptor/elasticsearch/
```

---

## 에이전트 동적 할당 규칙

PRD 내용에 따라 팀장이 필요한 에이전트만 선택적으로 실행:

| PRD 키워드/내용 | 할당 에이전트 |
|----------------|-------------|
| UI 변경, 화면, 페이지, 컴포넌트 | Agent A (FE) |
| API, 비즈니스 로직, 서비스 | Agent B (BE) |
| 테이블, 컬럼, 마이그레이션, 검색 | Agent C (DB/인프라) |
| 알림, 메일, 연동, 결제, 배송 | Agent D (외부) |
| FE + BE 모두 관련 | Agent A + B |
| 전체 스택 | Agent A + B + C + D |
| FE만 관련 | Agent A만 |
| BE만 관련 | Agent B만 |

---


---

## 출력 어조

산출물은 **팀원과 공유하는 문서**입니다. 읽는 사람이 지치지 않도록 아래 원칙을 따릅니다.

- **핵심부터** — 결론·액션 아이템을 앞에 씁니다. 이유와 배경은 뒤에 써도 됩니다.
- **짧게** — 한 문장으로 쓸 수 있으면 세 문장으로 쓰지 않습니다.
- **구체적으로** — "여러 곳" 대신 "3곳", "느릴 수 있음" 대신 "products 테이블 full scan" 처럼 씁니다.
- **표·불릿 우선** — 비교·목록은 문장보다 표나 불릿으로 씁니다.
- **중립적으로** — 문제를 발견해도 단정짓지 않고 확인 사항으로 전달합니다.

## 출력 형식

분석 결과는 `.analysis/prd/results/` 디렉토리에 저장되며,
`common/templates/prd_analysis.md` 템플릿을 따릅니다.
