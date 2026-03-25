# PR 리뷰 요청: GRT-6907

> 이 파일은 PR Review Server가 자동 생성했습니다.
> Claude Code에서 이 파일을 읽고 리뷰를 수행해주세요.
> 리뷰 완료 후 결과를 pr4400.json에 업데이트해주세요.

## PR 정보
- **제목**: [GRT-6907] - test: [BE] 코드 정리 - 통합 테스트 작성
- **URL**: https://github.com/doodlincorp/greeting-new-back/pull/4400
- **브랜치**: feature/GRT-6907-integration-test → dev
- **작성자**: dongseop-lee-doodlin
- **유형**: feature | **크기**: XL

## 변경 파일 (49개, +18561 -101)
- .github/workflows/ci_test.yml (+2 -2)
- build.gradle.kts (+4 -1)
- domain/build.gradle.kts (+1 -0)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/ApplicantsServiceImpl.kt (+1 -0)
- domain/src/main/kotlin/doodlin/greeting/common/VersionConstant.kt (+0 -1)
- domain/src/test/kotlin/doodlin/greeting/BaseIntegrationTest.kt (+71 -32)
- domain/src/test/kotlin/doodlin/greeting/BaseJunitIntegrationTest.kt (+131 -0)
- domain/src/test/kotlin/doodlin/greeting/aggregate/repository/NormalizedInternalAdapterTest.kt (+2 -2)
- domain/src/test/kotlin/doodlin/greeting/aggregate/service/ApplyAuthIntegrationTest.kt (+1530 -0)
- domain/src/test/kotlin/doodlin/greeting/aggregate/service/ApplyNoAuthIntegrationTest.kt (+1611 -0)
- domain/src/test/kotlin/doodlin/greeting/aggregate/service/NormalizedQueryServiceTest.kt (+2 -2)
- domain/src/test/kotlin/doodlin/greeting/aggregate/service/SimpleRuntimeConfigCacheTest.kt (+2 -2)
- domain/src/test/kotlin/doodlin/greeting/aggregate/service/TemporaryResumeSubmitIntegrationTest.kt (+1291 -0)
- domain/src/test/kotlin/doodlin/greeting/candidate/application/handler/ExamNumberEventHandlerIntegrationTest.kt (+44 -14)
- domain/src/test/kotlin/doodlin/greeting/candidate/application/repository/ApplicationDataIntegrationTest.kt (+2 -2)
- domain/src/test/kotlin/doodlin/greeting/candidate/application/service/ApplicantStageTransitionIntegrationTest.kt (+1427 -0)
- domain/src/test/kotlin/doodlin/greeting/candidate/application/service/GetApplicantResumeIntegrationTest.kt (+462 -0)
- domain/src/test/kotlin/doodlin/greeting/candidate/application/service/ModifyApplicationByApplicantIntegrationTest.kt (+1378 -0)
- domain/src/test/kotlin/doodlin/greeting/common/config/IntegrationTestMockConfig.kt (+149 -0)
- domain/src/test/kotlin/doodlin/greeting/common/config/MockResetHelper.kt (+15 -0)
- domain/src/test/kotlin/doodlin/greeting/common/testcontainer/TestContainers.kt (+152 -11)
- domain/src/test/kotlin/doodlin/greeting/evaluation/ai/service/AiScreeningIntegrationTest.kt (+1420 -0)
- domain/src/test/kotlin/doodlin/greeting/evaluation/evaluation/EvaluationKafkaPublisherTest.kt (+2 -2)
- domain/src/test/kotlin/doodlin/greeting/evaluation/evaluation/service/EvaluationContentServiceIntegrationTest.kt (+2158 -0)
- domain/src/test/kotlin/doodlin/greeting/evaluation/meeting/service/CreateMeetingIntegrationTest.kt (+1281 -0)
- domain/src/test/kotlin/doodlin/greeting/evaluation/meeting/service/MeetingChangeIntegrationTest.kt (+992 -0)
- domain/src/test/kotlin/doodlin/greeting/file/service/DocumentDownloadIntegrationTest.kt (+1592 -0)
- domain/src/test/kotlin/doodlin/greeting/file/service/DownloadableApplicantFetcherIntegrationTest.kt (+2 -2)
- domain/src/test/kotlin/doodlin/greeting/file/service/ExcelDownloadIntegrationTest.kt (+1181 -0)
- domain/src/test/kotlin/doodlin/greeting/fixture/ApplicationEditCommandFixture.kt (+601 -0)
- domain/src/test/kotlin/doodlin/greeting/fixture/OpeningFixture.kt (+3 -2)
- domain/src/test/kotlin/doodlin/greeting/opening/BaseOpeningIntegrationTest.kt (+5 -9)
- domain/src/test/kotlin/doodlin/greeting/opening/BaseOpeningJunitIntegrationTest.kt (+15 -0)
- domain/src/test/kotlin/doodlin/greeting/opening/opening/service/CreateOpeningInternalApplicationTest.kt (+5 -5)
- domain/src/test/kotlin/doodlin/greeting/opening/opening/service/OpeningCreationIntegrationTest.kt (+689 -0)
- domain/src/test/kotlin/doodlin/greeting/opening/opening/service/OpeningFacadeIntegrationTest.kt (+10 -6)
- domain/src/test/kotlin/doodlin/greeting/opening/opening/service/OpeningTranslationApplicationTest.kt (+2 -2)
- domain/src/test/kotlin/doodlin/greeting/opening/opening/service/OpeningUpdateIntegrationTest.kt (+322 -0)
- domain/src/test/kotlin/doodlin/greeting/opening/opening/service/OpeningsFacadeIntegrationTest.kt (+2 -2)
- domain/src/test/kotlin/doodlin/greeting/opening/opening/service/SampleOpeningServiceIntegrationTest.kt (+2 -2)
- domain/src/test/kotlin/doodlin/greeting/opening/recommendation/service/EmployeeReferralIntegrationTest.kt (+0 -0)
- domain/src/test/kotlin/integration_test_guide.MD (+0 -0)
- domain/src/test/resources/sql/init_ai_screening.sql (+0 -0)
- domain/src/test/resources/sql/init_applicant.sql (+0 -0)
- domain/src/test/resources/sql/init_evaluation.sql (+0 -0)
- domain/src/test/resources/sql/init_meeting.sql (+0 -0)
- domain/src/test/resources/sql/init_opening.sql (+0 -0)
- domain/src/test/resources/sql/init_workspace.sql (+0 -0)
- domain/src/test/resources/sql/seed.sql (+0 -0)

## 정적 분석 결과 (자동)
- [P2] Kafka 이벤트 변경 → Producer/Consumer 양쪽 호환성 확인 필요

## 리뷰 요청 사항
아래 관점으로 PR diff를 분석하고, 이 파일과 같은 디렉토리의 pr4400.json 파일의 inlineComments에 결과를 추가해주세요.

1. **비즈니스 로직**: PRD/TDD AC 충족 여부, 빠진 엣지 케이스
2. **버그 우려**: NPE, 동시성, 리소스 누수, 에러 삼킴, 데이터 유실
3. **성능 우려**: N+1, 페이지네이션, 트랜잭션 내 외부 호출
4. **보안**: SQL Injection, 권한 누락, 시크릿 노출
5. **PRD/TDD 누락**: 명시된 기능 미구현, API 스펙 불일치

## 관련 컨텍스트 위치
- PR diff: `gh pr diff 4400 --repo doodlincorp/greeting-new-back`
- 정적 분석: 같은 디렉토리의 `pr4400.json`
- .analysis/ 파이프라인 문서: 자동 매칭됨 (file-matcher)
