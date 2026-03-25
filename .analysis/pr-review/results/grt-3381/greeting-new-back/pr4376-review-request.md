# PR 리뷰 요청: GRT-3381

> 이 파일은 PR Review Server가 자동 생성했습니다.
> Claude Code에서 이 파일을 읽고 리뷰를 수행해주세요.
> 리뷰 완료 후 결과를 pr4376.json에 업데이트해주세요.

## PR 정보
- **제목**: [GRT-3381] - feat: 평가 할당 시 분산락 적용
- **URL**: https://github.com/doodlincorp/greeting-new-back/pull/4376
- **브랜치**: fix/grt-3381 → dev
- **작성자**: biuea3866
- **유형**: bugfix | **크기**: XL

## 변경 파일 (24개, +819 -85)
- domain/src/main/kotlin/doodlin/greeting/aggregate/service/MigrateApplicantsUseCase.kt (+3 -4)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/ApplicantsFacade.kt (+4 -2)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/ApplicantsServiceImpl.kt (+16 -17)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/ApplicationService.kt (+4 -5)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/AtsCreateApplicantUseCase.kt (+7 -8)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/BulkUploadApplicantsServiceImpl.kt (+3 -4)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/MoveProcessApplicantService.kt (+3 -4)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/RegisterApplicantService.kt (+3 -5)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/SampleApplicantService.kt (+7 -8)
- domain/src/main/kotlin/doodlin/greeting/common/common/apilock/annotation/DistributedLock.kt (+1 -0)
- domain/src/main/kotlin/doodlin/greeting/common/common/apilock/aspect/DistributedLockAspect.kt (+51 -11)
- domain/src/main/kotlin/doodlin/greeting/common/common/transaction/TransactionHelper.kt (+16 -0)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/event/AssignEvaluationRequestHandler.kt (+52 -0)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/event/RequestAssignEvaluationEvent.kt (+18 -0)
- domain/src/main/kotlin/doodlin/greeting/evaluation/evaluation/service/EvaluationService.kt (+8 -0)
- domain/src/main/kotlin/doodlin/greeting/integration/partner/service/IntegrateJobPlanetApplicantService.kt (+3 -4)
- domain/src/main/kotlin/doodlin/greeting/zconfuse/CommonControllerAdvice.kt (+16 -0)
- domain/src/test/kotlin/doodlin/greeting/candidate/application/service/ApplicationServiceCopyApplicantTest.kt (+6 -7)
- domain/src/test/kotlin/doodlin/greeting/candidate/application/service/CopyApplicantFlowIntegrationTest.kt (+3 -6)
- domain/src/test/kotlin/doodlin/greeting/common/common/apilock/aspect/DistributedLockAspectTest.kt (+197 -0)
- domain/src/test/kotlin/doodlin/greeting/common/config/DatabaseTestConfig.kt (+2 -0)
- domain/src/test/kotlin/doodlin/greeting/evaluation/evaluation/event/AssignEvaluationRequestHandlerTest.kt (+93 -0)
- domain/src/test/kotlin/doodlin/greeting/evaluation/evaluation/service/EvaluationServiceAssignConcurrencyTest.kt (+163 -0)
- domain/src/test/kotlin/doodlin/greeting/evaluation/evaluation/service/EvaluationServiceAssignTest.kt (+140 -0)

## 정적 분석 결과 (자동)
- [P3] API 변경이 3개 연관 레포에 영향 가능: greeting-aggregator, greeting-communication, greeting-api-gateway
- [ASK] [비즈니스] 다른 도메인 직접 참조: 다른 Bounded Context의 Repository를 직접 쓰면 도메인 간 결합이 생겨요.
- [ASK] [버그] !! (non-null assertion): !!은 NPE를 유발할 수 있어요. nullable 처리가 의도적인지 확인이 필요해요.
- [P2] ApplicantsFacade(Facade)에서 Repository를 직접 사용하고 있어요. Facade는 Service 호출만 해야 해요.
- [P2] ApplicantsFacade(Facade)에서 이벤트를 발행하고 있어요. 이벤트는 Service에서 발행해야 해요.
- [ASK] AssignEvaluationRequestHandler(evaluation)이 다른 도메인의 Repository를 직접 참조하고 있어요.

## 리뷰 요청 사항
아래 관점으로 PR diff를 분석하고, 이 파일과 같은 디렉토리의 pr4376.json 파일의 inlineComments에 결과를 추가해주세요.

1. **비즈니스 로직**: PRD/TDD AC 충족 여부, 빠진 엣지 케이스
2. **버그 우려**: NPE, 동시성, 리소스 누수, 에러 삼킴, 데이터 유실
3. **성능 우려**: N+1, 페이지네이션, 트랜잭션 내 외부 호출
4. **보안**: SQL Injection, 권한 누락, 시크릿 노출
5. **PRD/TDD 누락**: 명시된 기능 미구현, API 스펙 불일치

## 관련 컨텍스트 위치
- PR diff: `gh pr diff 4376 --repo doodlincorp/greeting-new-back`
- 정적 분석: 같은 디렉토리의 `pr4376.json`
- .analysis/ 파이프라인 문서: 자동 매칭됨 (file-matcher)
