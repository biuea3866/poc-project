# PR 리뷰 요청: GRT-6892

> 이 파일은 PR Review Server가 자동 생성했습니다.
> Claude Code에서 이 파일을 읽고 리뷰를 수행해주세요.
> 리뷰 완료 후 결과를 pr443.json에 업데이트해주세요.

## PR 정보
- **제목**: [GRT-6892] - fix: 지원서 양식 템플릿 권한 추가
- **URL**: https://github.com/doodlincorp/greeting-aggregator/pull/443
- **브랜치**: feature/GRT-6892 → dev
- **작성자**: junkwon-dev
- **유형**: feature | **크기**: M

## 변경 파일 (4개, +43 -1)
- adaptor/platform/src/main/kotlin/doodlin/greeting/aggregator/adaptor/platform/authorization/AuthorizationPayloadMapperExtensions.kt (+2 -0)
- buildSrc/src/main/kotlin/Dependencies.kt (+1 -1)
- business/application/authorization/src/main/kotlin/doodlin/greeting/aggregator/business/application/authorization/payload/Permission.kt (+1 -0)
- presentation/api/app/src/main/kotlin/doodlin/greeting/aggregator/presentation/api/app/controller/TemplateController.kt (+39 -0)

## 정적 분석 결과 (자동)
- [P3] API 변경이 2개 연관 레포에 영향 가능: greeting-new-back, greeting_payment-server

## 리뷰 요청 사항
아래 관점으로 PR diff를 분석하고, 이 파일과 같은 디렉토리의 pr443.json 파일의 inlineComments에 결과를 추가해주세요.

1. **비즈니스 로직**: PRD/TDD AC 충족 여부, 빠진 엣지 케이스
2. **버그 우려**: NPE, 동시성, 리소스 누수, 에러 삼킴, 데이터 유실
3. **성능 우려**: N+1, 페이지네이션, 트랜잭션 내 외부 호출
4. **보안**: SQL Injection, 권한 누락, 시크릿 노출
5. **PRD/TDD 누락**: 명시된 기능 미구현, API 스펙 불일치

## 관련 컨텍스트 위치
- PR diff: `gh pr diff 443 --repo doodlincorp/greeting-aggregator`
- 정적 분석: 같은 디렉토리의 `pr443.json`
- .analysis/ 파이프라인 문서: 자동 매칭됨 (file-matcher)
