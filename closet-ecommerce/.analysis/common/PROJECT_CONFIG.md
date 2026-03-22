# 프로젝트 설정

> Closet 의류 이커머스 프로젝트 공통 설정

## 외부 도구 연동

| 도구 | URL | 용도 |
|------|-----|------|
| **Jira** | https://biuea3866.atlassian.net/jira/software/projects/CP/boards/67 | 티켓 관리 (프로젝트 키: CP) |
| **Confluence** | https://biuea3866.atlassian.net/wiki/spaces/MFS/pages/1638401 | 문서 관리 (부모 페이지: 의류 이커머스 프로젝트) |

## Confluence 페이지 맵

| 문서 | 페이지 ID | URL |
|------|----------|-----|
| 부모 페이지 | 1638401 | /spaces/MFS/pages/1638401 |
| 기획서 | 1605634 | /spaces/MFS/pages/1605634 |
| PRD | 1638422 | /spaces/MFS/pages/1638422 |
| 로드맵 | 1671169 | /spaces/MFS/pages/1671169 |
| DDD 도메인 분석 | 1671183 | /spaces/MFS/pages/1671183 |
| ERD + 다이어그램 | 1736705 | /spaces/MFS/pages/1736705 |

## 문서화 규칙

- 모든 산출물은 **Confluence + 로컬 .md 파일** 양쪽에 기록
- Confluence: 팀 공유용 (시각화, 테이블 중심)
- .md 파일: 코드와 함께 버전 관리, 에이전트 참조용

## Jira 규칙

- 프로젝트 키: **CP**
- 티켓 넘버링: CP-{번호}
- 브랜치: `feature/cp-{번호}-{기능명}`
- 커밋 메시지: `[CP-{번호}] {작업 내용}`

## 작업 프로세스

```
Jira 티켓 생성 → 브랜치 생성 → 구현 → 테스트 → 커밋 → 푸시 → PR → Jira 업데이트 → Confluence 업데이트
```
