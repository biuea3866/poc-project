# PR 리뷰 요청: GRT-6888

> 이 파일은 PR Review Server가 자동 생성했습니다.
> Claude Code에서 이 파일을 읽고 리뷰를 수행해주세요.
> 리뷰 완료 후 결과를 pr4401.json에 업데이트해주세요.

## PR 정보
- **제목**: [GRT-6888] - feat: 지원서 목록 조회시 DISTINCT 조건 최소화
- **URL**: https://github.com/doodlincorp/greeting-new-back/pull/4401
- **브랜치**: feat/GRT-6888-2 → dev
- **작성자**: TaeWooKang
- **유형**: feature | **크기**: XL

## 변경 파일 (36개, +235 -1746)
- domain/src/main/kotlin/doodlin/greeting/aggregate/domain/GetTalentPoolV4Input.kt (+0 -22)
- domain/src/main/kotlin/doodlin/greeting/aggregate/service/KanbanApplicationService.kt (+1 -0)
- domain/src/main/kotlin/doodlin/greeting/aggregate/service/TableViewApplicationService.kt (+2 -0)
- domain/src/main/kotlin/doodlin/greeting/aggregate/service/TalentPoolApplicationService.kt (+0 -17)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/domain/ApplicantsServiceCommand.kt (+2 -0)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/domain/AtsApplicantFilter.kt (+26 -16)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/domain/GetMobileKanbanDataCommandV3.kt (+0 -10)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/domain/GetMobileKanbanDataCountV3.kt (+0 -7)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/repository/ApplicantJpaRepository.kt (+2 -0)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/repository/ApplicantsReader.kt (+0 -12)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/repository/ApplicantsReaderImpl.kt (+0 -23)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/repository/AtsApplicantReader.kt (+1 -79)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/repository/AtsApplicantReaderImpl.kt (+4 -167)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/repository/AtsApplicantRepositoryCustom.kt (+0 -81)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/repository/AtsApplicantRepositoryCustomImpl.kt (+5 -335)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/repository/JPAQueryApplicantFilterExtension.kt (+21 -153)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/repository/QApplicantsRepository.kt (+0 -12)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/repository/QApplicantsRepositoryImpl.kt (+0 -38)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/ApplicantApplicationService.kt (+2 -0)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/ApplicantsFacade.kt (+2 -0)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/ApplicantsServiceImpl.kt (+21 -8)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/MobileKanbanDataBuilder.kt (+21 -19)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/MobileKanbanDataBuilderV3.kt (+2 -0)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/OpeningApplicantsCountService.kt (+0 -23)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/PassedApplicantsIdsService.kt (+12 -9)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/PassedApplicantsListService.kt (+21 -24)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/ProcessApplicantIdsService.kt (+9 -6)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/ProcessApplicantListBuilderV2.kt (+17 -11)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/RejectedApplicantIdsService.kt (+7 -22)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/RejectedApplicantListBuilder.kt (+13 -9)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/TableViewHeaderService.kt (+44 -20)
- domain/src/main/kotlin/doodlin/greeting/candidate/application/service/TalentPoolApplicantListBuilder.kt (+0 -335)
- domain/src/main/kotlin/doodlin/greeting/opening/opening/domain/GetTalentPoolCommandV4.kt (+0 -13)
- domain/src/main/kotlin/doodlin/greeting/opening/workspace/service/TalentPoolService.kt (+0 -17)
- interfaces/src/main/kotlin/doodlin/greeting/aggregate/controller/OldTalentPoolController.kt (+0 -35)
- interfaces/src/test/kotlin/doodlin/greeting/aggregate/controller/OldTalentPoolControllerTest.kt (+0 -223)

## 정적 분석 결과 (자동)
- [P3] API 변경이 3개 연관 레포에 영향 가능: greeting-aggregator, greeting-communication, greeting-api-gateway

## 리뷰 요청 사항
아래 관점으로 PR diff를 분석하고, 이 파일과 같은 디렉토리의 pr4401.json 파일의 inlineComments에 결과를 추가해주세요.

1. **비즈니스 로직**: PRD/TDD AC 충족 여부, 빠진 엣지 케이스
2. **버그 우려**: NPE, 동시성, 리소스 누수, 에러 삼킴, 데이터 유실
3. **성능 우려**: N+1, 페이지네이션, 트랜잭션 내 외부 호출
4. **보안**: SQL Injection, 권한 누락, 시크릿 노출
5. **PRD/TDD 누락**: 명시된 기능 미구현, API 스펙 불일치

## 관련 컨텍스트 위치
- PR diff: `gh pr diff 4401 --repo doodlincorp/greeting-new-back`
- 정적 분석: 같은 디렉토리의 `pr4401.json`
- .analysis/ 파이프라인 문서: 자동 매칭됨 (file-matcher)
