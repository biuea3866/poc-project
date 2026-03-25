# PR 리뷰 요청: GRT-6893

> 이 파일은 PR Review Server가 자동 생성했습니다.
> Claude Code에서 이 파일을 읽고 리뷰를 수행해주세요.
> 리뷰 완료 후 결과를 pr4394.json에 업데이트해주세요.

## PR 정보
- **제목**: [GRT-6893] - refactor: 지원자 권한 추가 이후에 예정이던 TODO 주석에 대한 작업 진행
- **URL**: https://github.com/doodlincorp/greeting-new-back/pull/4394
- **브랜치**: refactor/GRT-6893 → dev
- **작성자**: TaeWooKang
- **유형**: refactor | **크기**: XL

## 변경 파일 (58개, +186 -3254)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/domain/ApplicantsFacadeInput.kt (+0 -34)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/domain/ApplicantsFacadeOutput.kt (+0 -68)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/domain/GetApplicantResumeOutputLegacy.kt (+0 -409)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/repository/ApplicantPersistenceAdapter.kt (+20 -0)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/repository/AtsApplicantRepositoryCustom.kt (+12 -0)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/repository/AtsApplicantRepositoryCustomImpl.kt (+44 -0)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/ApplicantApplicationService.kt (+0 -2)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/ApplicantQueryPort.kt (+12 -0)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/ApplicantsFacade.kt (+2 -437)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/GetApplicantResumeApplication.kt (+0 -107)
- domain/src/main/kotlin/doodlin/greeting/common/VersionConstant.kt (+0 -1)
- domain/src/main/kotlin/doodlin/greeting/common/web/CommonWebMvcConfig.kt (+0 -1)
- domain/src/main/kotlin/doodlin/greeting/communication/repository/EmailLogsOnApplicantReader.kt (+6 -6)
- domain/src/main/kotlin/doodlin/greeting/communication/repository/EmailLogsOnApplicantReaderImpl.kt (+12 -12)
- domain/src/main/kotlin/doodlin/greeting/communication/repository/ReservingMailsReader.kt (+2 -2)
- domain/src/main/kotlin/doodlin/greeting/communication/repository/ReservingMailsReaderImpl.kt (+4 -4)
- domain/src/main/kotlin/doodlin/greeting/communication/service/CheckApplicantPermissionService.kt (+17 -236)
- domain/src/main/kotlin/doodlin/greeting/communication/service/MailServiceImpl.kt (+44 -48)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/domain/ApplicantAverageScoreResult.kt (+0 -31)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/domain/ChangeEvaluationInput.kt (+0 -96)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/domain/EvaluationResult.kt (+0 -7)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/domain/GetApplicantAverageScoreCommand.kt (+0 -7)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/domain/GetApplicantAverageScoreInput.kt (+0 -9)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/domain/GetApplicantAverageScoreOutput.kt (+0 -88)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/domain/GetEvaluationScoreHistoryInput.kt (+0 -8)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/domain/GetEvaluationScoreHistoryOutput.kt (+0 -118)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/domain/GetEvaluationsInput.kt (+0 -8)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/domain/GetEvaluationsOutput.kt (+0 -7)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/service/ChangeEvaluationApplication.kt (+0 -105)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/service/CreateEvaluationApplication.kt (+0 -120)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/service/GetApplicantAverageScoreApplication.kt (+0 -49)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/service/GetApplicantAverageScoreService.kt (+0 -72)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/service/GetApplicantAverageScoreUseCase.kt (+0 -8)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/service/GetEvaluationScoreHistoryApplication.kt (+0 -75)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/service/GetEvaluationsApplication.kt (+1 -88)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/service/GetEvaluationsService.kt (+0 -43)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/service/GetEvaluationsUseCase.kt (+0 -8)
- domain/src/main/kotlin/doodlin/greeting/evaluation/meeting/domain/MeetingTimeWithId.kt (+0 -0)
- domain/src/main/kotlin/doodlin/greeting/opening/opening/domain/OpeningsFacadeInput.kt (+1 -2)
- domain/src/main/kotlin/doodlin/greeting/opening/opening/service/OpeningsFacade.kt (+4 -20)
- domain/src/main/kotlin/doodlin/greeting/opening/opening/service/OpeningsServiceImpl.kt (+0 -3)
- domain/src/main/kotlin/doodlin/greeting/opening/workspace/domain/GreetingRole.kt (+0 -1)
- domain/src/main/kotlin/doodlin/greeting/zconfuse/applicants/ApplicantsController.kt (+2 -142)
- domain/src/main/kotlin/doodlin/greeting/zconfuse/applicants/request/ApplicantsControllerRequest.kt (+0 -5)
- domain/src/main/kotlin/doodlin/greeting/zconfuse/workspaces/requests/WorkspaceRequest.kt (+1 -8)
- domain/src/test/kotlin/doodlin/greeting/evaluation/evaluation/service/GetEvaluationsApplicationApplyCurrentProcessTest.kt (+0 -2)
- domain/src/test/kotlin/doodlin/greeting/zconfuse/applicants/ApplicantsControllerTest.kt (+1 -165)
- domain/src/test/kotlin/doodlin/greeting/zconfuse/openings/OpeningsControllerTest.kt (+1 -2)
- interfaces/src/main/kotlin/doodlin/greeting/candidate/controller/ApplicantApiController.kt (+0 -30)
- interfaces/src/main/kotlin/doodlin/greeting/evaluation/controller/ChangeEvaluationRequest.kt (+0 -67)
- interfaces/src/main/kotlin/doodlin/greeting/evaluation/controller/CreateEvaluationRequest.kt (+0 -68)
- interfaces/src/main/kotlin/doodlin/greeting/evaluation/controller/EvaluationApiController.kt (+0 -174)
- interfaces/src/main/kotlin/doodlin/greeting/evaluation/controller/GetEvaluationScoreHistoryResponse.kt (+0 -15)
- interfaces/src/main/kotlin/doodlin/greeting/evaluation/controller/MeetingController.kt (+0 -1)
- interfaces/src/main/kotlin/doodlin/greeting/evaluation/controller/MeetingTimeSlotController.kt (+0 -1)
- interfaces/src/test/kotlin/doodlin/greeting/candidate/controller/ApplicantApiControllerTest.kt (+0 -60)
- interfaces/src/test/kotlin/doodlin/greeting/evaluation/controller/EvaluationApiControllerRequestFixtures.kt (+0 -35)
- interfaces/src/test/kotlin/doodlin/greeting/evaluation/controller/EvaluationApiControllerTest.kt (+0 -139)

## 정적 분석 결과 (자동)
- [P3] API 변경이 3개 연관 레포에 영향 가능: greeting-aggregator, greeting-communication, greeting-api-gateway
- [P2] ApplicantsFacade(Facade)에서 Repository를 직접 사용하고 있어요. Facade는 Service 호출만 해야 해요.
- [P2] EvaluationApiController가 Service를 직접 주입하고 있어요. Facade만 주입해야 해요.

## 리뷰 요청 사항
아래 관점으로 PR diff를 분석하고, 이 파일과 같은 디렉토리의 pr4394.json 파일의 inlineComments에 결과를 추가해주세요.

1. **비즈니스 로직**: PRD/TDD AC 충족 여부, 빠진 엣지 케이스
2. **버그 우려**: NPE, 동시성, 리소스 누수, 에러 삼킴, 데이터 유실
3. **성능 우려**: N+1, 페이지네이션, 트랜잭션 내 외부 호출
4. **보안**: SQL Injection, 권한 누락, 시크릿 노출
5. **PRD/TDD 누락**: 명시된 기능 미구현, API 스펙 불일치

## 관련 컨텍스트 위치
- PR diff: `gh pr diff 4394 --repo doodlincorp/greeting-new-back`
- 정적 분석: 같은 디렉토리의 `pr4394.json`
- .analysis/ 파이프라인 문서: 자동 매칭됨 (file-matcher)
