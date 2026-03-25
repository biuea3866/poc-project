# PR 리뷰 요청: GRT-6907

> 이 파일은 PR Review Server가 자동 생성했습니다.
> Claude Code에서 이 파일을 읽고 리뷰를 수행해주세요.
> 리뷰 완료 후 결과를 pr424.json에 업데이트해주세요.

## PR 정보
- **제목**: [GRT-6907] - test: [BE] 코드 정리 - 통합 테스트 작성
- **URL**: https://github.com/doodlincorp/greeting-ats/pull/424
- **브랜치**: feature/GRT-6907-integration-test → dev
- **작성자**: dongseop-lee-doodlin
- **유형**: feature | **크기**: XL

## 변경 파일 (22개, +4490 -0)
- greeting-recruit-applicant/adaptor/mysql/build.gradle.kts (+10 -0)
- greeting-recruit-applicant/adaptor/mysql/src/test/kotlin/doodlin/greeting/recruit/applicant/adaptor/mysql/account/repository/ApplicantAccountApplicationSubmitRepositoryTest.kt (+416 -0)
- greeting-recruit-applicant/adaptor/mysql/src/test/kotlin/doodlin/greeting/recruit/applicant/adaptor/mysql/account/repository/ApplicantAccountTemporaryResumeQueryRepositoryTest.kt (+511 -0)
- greeting-recruit-applicant/adaptor/mysql/src/test/kotlin/doodlin/greeting/recruit/applicant/adaptor/mysql/account/repository/ApplicantAccountTemporaryResumeRepositoryTest.kt (+287 -0)
- greeting-recruit-applicant/business/domain/src/test/kotlin/doodlin/greeting/recruit/applicant/business/domain/auth/ApplicantAccountAuthJwtServiceTest.kt (+1 -0)
- greeting-recruit-applicant/presentation/api/build.gradle.kts (+19 -0)
- greeting-recruit-applicant/presentation/api/src/test/kotlin/doodlin/greeting/recruit/applicant/api/account/controller/RecruitApplicantAccountTemporaryResumeApiControllerTest.kt (+764 -0)
- greeting-recruit-applicant/presentation/api/src/test/kotlin/doodlin/greeting/recruit/applicant/api/account/controller/internal/InternalApplicantAccountApplicationApiControllerTest.kt (+349 -0)
- greeting-recruit-applicant/presentation/api/src/test/kotlin/doodlin/greeting/recruit/applicant/api/account/controller/internal/InternalApplicantAccountTemporaryResumeApiControllerTest.kt (+227 -0)
- greeting-recruit-applicant/presentation/api/src/test/kotlin/doodlin/greeting/recruit/applicant/api/integration/AbstractIntegrationTest.kt (+138 -0)
- greeting-recruit-applicant/presentation/api/src/test/kotlin/doodlin/greeting/recruit/applicant/api/integration/ApplicationQueryIntegrationTest.kt (+207 -0)
- greeting-recruit-applicant/presentation/api/src/test/kotlin/doodlin/greeting/recruit/applicant/api/integration/ApplicationSubmitIntegrationTest.kt (+197 -0)
- greeting-recruit-applicant/presentation/api/src/test/kotlin/doodlin/greeting/recruit/applicant/api/integration/ApplyHistoryResubmitIntegrationTest.kt (+129 -0)
- greeting-recruit-applicant/presentation/api/src/test/kotlin/doodlin/greeting/recruit/applicant/api/integration/TemporaryResumeInitV4IntegrationTest.kt (+123 -0)
- greeting-recruit-applicant/presentation/api/src/test/kotlin/doodlin/greeting/recruit/applicant/api/integration/TemporaryResumeQueryGetV4IntegrationTest.kt (+124 -0)
- greeting-recruit-applicant/presentation/api/src/test/kotlin/doodlin/greeting/recruit/applicant/api/integration/TemporaryResumeUpdateV4IntegrationTest.kt (+176 -0)
- greeting-recruit-applicant/presentation/api/src/test/kotlin/doodlin/greeting/recruit/applicant/api/integration/config/IntegrationTestComponentExcludeFilter.kt (+35 -0)
- greeting-recruit-applicant/presentation/api/src/test/kotlin/doodlin/greeting/recruit/applicant/api/integration/config/TestApplication.kt (+21 -0)
- greeting-recruit-applicant/presentation/api/src/test/kotlin/doodlin/greeting/recruit/applicant/api/integration/config/TestMongoConfig.kt (+114 -0)
- greeting-recruit-applicant/presentation/api/src/test/kotlin/doodlin/greeting/recruit/applicant/api/integration/fixture/TemporaryResumeFixtures.kt (+478 -0)
- greeting-recruit-applicant/presentation/api/src/test/resources/application-integration-test.yml (+123 -0)
- greeting-recruit-applicant/presentation/api/src/test/resources/application.yml (+41 -0)

## 정적 분석 결과 (자동)
없음

## 리뷰 요청 사항
아래 관점으로 PR diff를 분석하고, 이 파일과 같은 디렉토리의 pr424.json 파일의 inlineComments에 결과를 추가해주세요.

1. **비즈니스 로직**: PRD/TDD AC 충족 여부, 빠진 엣지 케이스
2. **버그 우려**: NPE, 동시성, 리소스 누수, 에러 삼킴, 데이터 유실
3. **성능 우려**: N+1, 페이지네이션, 트랜잭션 내 외부 호출
4. **보안**: SQL Injection, 권한 누락, 시크릿 노출
5. **PRD/TDD 누락**: 명시된 기능 미구현, API 스펙 불일치

## 관련 컨텍스트 위치
- PR diff: `gh pr diff 424 --repo doodlincorp/greeting-ats`
- 정적 분석: 같은 디렉토리의 `pr424.json`
- .analysis/ 파이프라인 문서: 자동 매칭됨 (file-matcher)
