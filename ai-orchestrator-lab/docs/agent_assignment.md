# Agent Assignment Guide

## 원칙

이 프로젝트는 "한 에이전트가 한 티켓을 끝까지 책임지는 방식"으로 운영합니다.

- 티켓마다 `owner_role`을 하나만 둡니다.
- 구현 도중 다른 역할이 개입하더라도 최종 책임자는 바뀌지 않습니다.
- 리뷰와 승인 역할은 구현 역할과 분리합니다.

## 역할 정의

- `pm`
  PRD, 로드맵, 요구사항, Confluence 반영 책임
- `architect`
  기술 선택, 구조 결정, cross-lane 인터페이스 검토 책임
- `tech_lead`
  티켓 분해, 우선순위, 병렬 작업 조정 책임
- `be_engineer`
  백엔드 구현 티켓 end-to-end 책임
- `fe_engineer`
  프론트엔드 구현 티켓 end-to-end 책임
- `devops_engineer`
  인프라/관측성 티켓 end-to-end 책임
- `qa`
  품질 게이트, 리뷰 승인 책임

## 운영 규칙

1. 티켓은 항상 `owner_role`을 가진다.
2. `reviewer_role`은 기본적으로 `qa` 또는 `architect` 중 하나를 둔다.
3. cross-lane 티켓은 `owner_role` 하나를 두고, 협업 대상은 별도 문서나 티켓 설명에 적는다.
4. 구현 완료, 문서 반영, 리뷰 통과 전에는 owner가 티켓을 닫지 않는다.

## 추천 배정 규칙

- `lane=be` -> `owner_role=be_engineer`
- `lane=fe` -> `owner_role=fe_engineer`
- `lane=devops` -> `owner_role=devops_engineer`
- `lane=common` -> `owner_role=tech_lead` 또는 `pm`

## 머지 흐름

1. owner가 티켓 작업 수행
2. 필요 시 Confluence 기술 문서/PRD/Jira 갱신
3. reviewer 통과
4. PR 생성
5. 최종 머지는 사용자 수행
