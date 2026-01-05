# 🛒 Open Market Platform

B2C/C2C 오픈마켓 플랫폼 포트폴리오 프로젝트

## 프로젝트 개요

다양한 판매자(개인/기업)가 상품을 등록하고 구매자가 상품을 구매할 수 있는 온라인 마켓플레이스

## 기술 스택

### Backend (Human 담당)
- Kotlin + Spring Boot 3.2+
- MySQL 8.0, Redis 7.0, Kafka
- Elasticsearch 8.x
- JPA + QueryDSL

### Frontend (LLM 담당)
- Next.js 14+ (App Router)
- TypeScript
- Tailwind CSS + shadcn/ui
- Zustand + React Query

### Infrastructure (LLM 담당)
- Docker Compose
- Pinpoint (APM)
- k6 (부하테스트)
- GitHub Actions (CI/CD)

## 프로젝트 구조

```
open-market/
├── docs/                    # 기획/설계 문서
├── backend/                 # 백엔드 (Kotlin + Spring Boot)
│   ├── api/                 # API 모듈
│   ├── domain/              # 도메인 모듈
│   ├── infra/               # 인프라 모듈
│   └── batch/               # 배치 모듈
├── frontend/                # 프론트엔드 (Next.js)
├── infra/                   # 인프라 설정
│   ├── docker/              # Docker Compose
│   ├── k6/                  # 부하 테스트
│   └── .github/workflows/   # CI/CD
└── mock-servers/            # Mock 서버
    ├── pg-mock/             # PG사 Mock
    └── channel-mock/        # 외부 채널 Mock
```

## 시작하기

### 1. 인프라 실행
```bash
cd infra/docker
docker-compose up -d
```

### 2. 백엔드 실행
```bash
cd backend
./gradlew bootRun
```

### 3. 프론트엔드 실행
```bash
cd frontend
npm install
npm run dev
```

## 문서

| 문서 | 설명 |
|------|------|
| [SERVICE_FLOW.md](docs/SERVICE_FLOW.md) | 전체 서비스 플로우 |
| [WORK_PLAN.md](docs/WORK_PLAN.md) | 작업 순서 계획 |
| [CONTEXT.md](docs/CONTEXT.md) | 현재 진행 상황 |
| [HISTORY.md](docs/HISTORY.md) | 작업 히스토리 |

## 담당자

| 역할 | 담당 | 범위 |
|------|------|------|
| Backend | Human | 도메인 설계, API 개발 |
| Frontend | LLM | 구매자/셀러/관리자 UI |
| Infrastructure | LLM | Docker, k6, Pinpoint |
| Mock Server | LLM | PG, Channel Mock |

## 라이선스

Private - Portfolio Project
