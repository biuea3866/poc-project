# Jira 동기화 가이드

## 원칙

md 티켓 파일이 Source of Truth입니다. Jira는 md를 반영하는 뷰입니다.

## 매핑 규칙

| md 필드 | Jira 필드 | 비고 |
|---------|----------|------|
| 제목 | Summary | 접두사 규칙 적용 (아래 참조) |
| 메타 > 사이즈 | Start date + Due date | `customfield_10015` + `duedate` |
| 메타 > 의존 | Issue Link | "is blocked by"로 연결 |
| 작업 내용 | Description 본문 | |
| 다이어그램 | Description 이미지 | PNG 변환 후 첨부파일 업로드 |
| 테스트 케이스 | Description 하단 | 한 문장 |

## Summary 접두사

- plan-data-processor 티켓: `[plan-worker]` 접두사를 붙입니다.
- 그 외 서비스별 접두사는 프로젝트 컨벤션을 따릅니다.

## 날짜 설정

사이즈로 기간을 계산합니다.
- XS=0.5일, S=1일, M=2일, L=3일
- Start date(`customfield_10015`)와 Due date(`duedate`)를 설정합니다.

## Issue Link

의존 티켓을 "is blocked by"로 연결합니다.

```
A-10 (의존: A-03~A-09)
→ A-10 is blocked by A-03
→ A-10 is blocked by A-04
→ ... (각각 개별 연결)
```

## 다이어그램 처리

1. Mermaid 코드를 PNG로 변환합니다: `mmdc -i input.mmd -o output.png -w 2400 -b white -t default -s 2`
2. Jira 티켓에 첨부파일로 업로드합니다.
3. Description에서 `!{파일명}.png|thumbnail!` 형식으로 삽입합니다.

## Description 템플릿

```
## 작업 내용
{md 파일의 작업 내용 섹션}

## 다이어그램
!{티켓ID}_flow.png|thumbnail!
!{티켓ID}_class.png|thumbnail!

## 테스트 케이스
{한 문장}
```

## 생성 순서

의존이 없는 티켓부터 생성합니다.
1. 의존 없는 티켓 먼저 생성 → Jira Key 확보
2. 의존 티켓 생성 시 blocked by 링크 연결

## Confluence 동기화

Jira 티켓과 함께 Confluence 페이지도 동기화합니다. 상세는 [document-sync.md](document-sync.md)를 참조합니다.
