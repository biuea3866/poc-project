# 문의 대응 보고서

> 일시: {날짜}
> 티켓: {티켓 번호}
> 유형: {버그/동작원리/데이터/성능/권한}
> 긴급도: {Critical/High/Medium/Low}

---

## 1. 문의 내용

| 항목 | 내용 |
|------|------|
| 보고자 | |
| 증상 | |
| 재현 경로 | |
| 환경 | dev/stage/prod |
| 발생 시점 | |

---

## 2. 원인 분석

### 2.1 근본 원인 (Root Cause)

> 한 줄 요약:

### 2.2 코드 추적 경로

```
[진입점]
FE: path/component.tsx:L{line} → API 호출
     ↓
[Gateway]
closet-api-gateway → 라우팅: /api/v1/...
     ↓
[Controller]
closet-{service}/module/path/Controller.kt:L{line}
     ↓
[Service]
closet-{service}/module/path/Service.kt:L{line}
     ↓
[원인 발생 지점]
closet-{service}/module/path/File.kt:L{line} ← 여기서 문제 발생
```

### 2.3 원인 상세

> 왜 이런 현상이 발생하는지 상세 설명

---

## 3. 영향 범위

| 범위 | 내용 |
|------|------|
| 영향 받는 사용자 | 전체/특정 등급/특정 셀러 |
| 영향 받는 기능 | |
| 데이터 영향 | 있음/없음 |

---

## 4. 해결 방안

### 4.1 즉시 조치 (임시)

> 있는 경우에만 작성

### 4.2 근본 해결

| 대상 파일 | 수정 내용 |
|----------|----------|
| `closet-{service}/path/file:L{line}` | 수정 방향 |

### 4.3 사이드 이펙트

| 항목 | 영향 여부 |
|------|----------|
| 다른 API | |
| Kafka 이벤트 | |
| 다른 서비스 | |

---

## 5. 재발 방지

> 테스트 추가, 유효성 검증 강화 등
