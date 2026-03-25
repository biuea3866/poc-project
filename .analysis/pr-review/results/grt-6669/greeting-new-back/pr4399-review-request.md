# PR 리뷰 요청: GRT-6669

> 이 파일은 PR Review Server가 자동 생성했습니다.
> Claude Code에서 이 파일을 읽고 리뷰를 수행해주세요.
> 리뷰 완료 후 결과를 pr4399.json에 업데이트해주세요.

## PR 정보
- **제목**: [GRT-6669] - feat: kafka 2.0.19 상향
- **URL**: https://github.com/doodlincorp/greeting-new-back/pull/4399
- **브랜치**: codex/feature/GRT-6669_kafka_2_0_19_upgrade → dev
- **작성자**: junney-jang
- **유형**: feature | **크기**: S

## 변경 파일 (2개, +42 -1)
- build.gradle.kts (+1 -1)
- domain/src/test/kotlin/doodlin/greeting/common/config/WebClientConfigTest.kt (+41 -0)

## 정적 분석 결과 (자동)
없음

## 리뷰 요청 사항
아래 관점으로 PR diff를 분석하고, 이 파일과 같은 디렉토리의 pr4399.json 파일의 inlineComments에 결과를 추가해주세요.

1. **비즈니스 로직**: PRD/TDD AC 충족 여부, 빠진 엣지 케이스
2. **버그 우려**: NPE, 동시성, 리소스 누수, 에러 삼킴, 데이터 유실
3. **성능 우려**: N+1, 페이지네이션, 트랜잭션 내 외부 호출
4. **보안**: SQL Injection, 권한 누락, 시크릿 노출
5. **PRD/TDD 누락**: 명시된 기능 미구현, API 스펙 불일치

## 관련 컨텍스트 위치
- PR diff: `gh pr diff 4399 --repo doodlincorp/greeting-new-back`
- 정적 분석: 같은 디렉토리의 `pr4399.json`
- .analysis/ 파이프라인 문서: 자동 매칭됨 (file-matcher)
