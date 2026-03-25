# PR 리뷰 요청: GRT-6878

> 이 파일은 PR Review Server가 자동 생성했습니다.
> Claude Code에서 이 파일을 읽고 리뷰를 수행해주세요.
> 리뷰 완료 후 결과를 pr442.json에 업데이트해주세요.

## PR 정보
- **제목**: [GRT-6878] - feat: 원데이 면접 api 추가
- **URL**: https://github.com/doodlincorp/greeting-aggregator/pull/442
- **브랜치**: feature/grt-6878 → dev
- **작성자**: biuea3866
- **유형**: feature | **크기**: L

## 변경 파일 (14개, +298 -0)
- adaptor/opening/src/main/kotlin/doodlin/greeting/aggregator/adaptor/opening/OpeningNewBackFeignClient.kt (+5 -0)
- adaptor/opening/src/main/kotlin/doodlin/greeting/aggregator/adaptor/opening/WorkspaceLabAdaptor.kt (+16 -0)
- adaptor/opening/src/main/kotlin/doodlin/greeting/aggregator/adaptor/opening/payload/response/GetWorkspaceLabFunction.kt (+19 -0)
- adaptor/opening/src/test/kotlin/doodlin/greeting/aggregator/adaptor/opening/WorkspaceLabAdaptorTest.kt (+65 -0)
- adaptor/opening/src/test/kotlin/doodlin/greeting/aggregator/adaptor/opening/payload/response/GetWorkspaceLabFunctionTest.kt (+77 -0)
- business/application/greeting/src/main/kotlin/doodlin/greeting/aggregator/business/application/greeting/candidate/payload/result/MaskedApplicationResult.kt (+6 -0)
- business/application/greeting/src/main/kotlin/doodlin/greeting/aggregator/business/application/greeting/candidate/port/in/GetCandidateDetailInputPort.kt (+1 -0)
- business/application/greeting/src/main/kotlin/doodlin/greeting/aggregator/business/application/greeting/opening/payload/WorkspaceLaboratoryFunction.kt (+17 -0)
- business/application/greeting/src/main/kotlin/doodlin/greeting/aggregator/business/application/greeting/opening/port/in/WorkspaceLaboratoryQueryInputPort.kt (+15 -0)
- business/application/greeting/src/main/kotlin/doodlin/greeting/aggregator/business/application/greeting/opening/port/out/WorkspaceLabQueryOutputPort.kt (+10 -0)
- business/application/greeting/src/main/kotlin/doodlin/greeting/aggregator/business/application/greeting/opening/usecase/WorkspaceLaboratoryUseCase.kt (+10 -0)
- presentation/api/app/src/main/kotlin/doodlin/greeting/aggregator/presentation/api/app/controller/ApplicantController.kt (+52 -0)
- presentation/api/app/src/main/kotlin/doodlin/greeting/aggregator/presentation/api/app/controller/ProxyController.kt (+2 -0)
- presentation/api/app/src/test/kotlin/doodlin/greeting/aggregator/presentation/api/app/controller/ApplicantControllerTest.kt (+3 -0)

## 정적 분석 결과 (자동)
- [P3] API 변경이 2개 연관 레포에 영향 가능: greeting-new-back, greeting_payment-server

## 리뷰 요청 사항
아래 관점으로 PR diff를 분석하고, 이 파일과 같은 디렉토리의 pr442.json 파일의 inlineComments에 결과를 추가해주세요.

1. **비즈니스 로직**: PRD/TDD AC 충족 여부, 빠진 엣지 케이스
2. **버그 우려**: NPE, 동시성, 리소스 누수, 에러 삼킴, 데이터 유실
3. **성능 우려**: N+1, 페이지네이션, 트랜잭션 내 외부 호출
4. **보안**: SQL Injection, 권한 누락, 시크릿 노출
5. **PRD/TDD 누락**: 명시된 기능 미구현, API 스펙 불일치

## 관련 컨텍스트 위치
- PR diff: `gh pr diff 442 --repo doodlincorp/greeting-aggregator`
- 정적 분석: 같은 디렉토리의 `pr442.json`
- .analysis/ 파이프라인 문서: 자동 매칭됨 (file-matcher)
