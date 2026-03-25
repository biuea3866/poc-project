# PR 리뷰 요청: GRT-6669

> 이 파일은 PR Review Server가 자동 생성했습니다.
> Claude Code에서 이 파일을 읽고 리뷰를 수행해주세요.
> 리뷰 완료 후 결과를 pr414.json에 업데이트해주세요.

## PR 정보
- **제목**: [GRT-6669] - feat: trace/logging 기준 반영 및 async/retrofit 전파 보강
- **URL**: https://github.com/doodlincorp/greeting-ats/pull/414
- **브랜치**: feat/GRT-6669 → dev
- **작성자**: junney-jang
- **유형**: feature | **크기**: XL

## 변경 파일 (58개, +386 -52)
- buildSrc/src/main/kotlin/Dependencies.kt (+4 -0)
- greeting-ats/adaptor/kafka/build.gradle.kts (+2 -2)
- greeting-ats/adaptor/kafka/src/main/resources/logback/logback-json-console.xml (+0 -0)
- greeting-ats/adaptor/kafka/src/main/resources/logback/logback-text-console.xml (+0 -0)
- greeting-ats/adaptor/retrofit/build.gradle.kts (+2 -1)
- greeting-ats/adaptor/retrofit/src/main/kotlin/doodlin/greeting/ats/adaptor/retrofit/applicant/config/ApplicantApiConfig.kt (+3 -1)
- greeting-ats/adaptor/retrofit/src/main/kotlin/doodlin/greeting/ats/adaptor/retrofit/applicant_account/config/RecruitApplicantApiConfig.kt (+3 -1)
- greeting-ats/adaptor/retrofit/src/main/kotlin/doodlin/greeting/ats/adaptor/retrofit/authz/config/AuthzApiConfig.kt (+3 -1)
- greeting-ats/adaptor/retrofit/src/main/kotlin/doodlin/greeting/ats/adaptor/retrofit/common/config/TracingOkHttpConfiguration.kt (+48 -0)
- greeting-ats/adaptor/retrofit/src/main/kotlin/doodlin/greeting/ats/adaptor/retrofit/crypto/config/CryptoApiConfig.kt (+4 -2)
- greeting-ats/adaptor/retrofit/src/main/kotlin/doodlin/greeting/ats/adaptor/retrofit/integration/kribs/config/KirbsIntegrationRetrofitConfig.kt (+4 -2)
- greeting-ats/adaptor/retrofit/src/main/kotlin/doodlin/greeting/ats/adaptor/retrofit/meeting/config/MeetingApiConfig.kt (+3 -1)
- greeting-ats/adaptor/retrofit/src/main/kotlin/doodlin/greeting/ats/adaptor/retrofit/user/config/UserApiConfig.kt (+3 -1)
- greeting-ats/presentation/api/build.gradle.kts (+2 -1)
- greeting-ats/presentation/api/src/main/resources/logback-spring.xml (+4 -4)
- greeting-ats/presentation/api/src/main/resources/logback/logback-json-console.xml (+0 -0)
- greeting-ats/presentation/api/src/main/resources/logback/logback-text-console.xml (+0 -0)
- greeting-ats/presentation/batch/build.gradle.kts (+1 -0)
- greeting-ats/presentation/batch/src/main/resources/logback-spring.xml (+4 -4)
- greeting-ats/presentation/batch/src/main/resources/logback/logback-json-console.xml (+0 -0)
- greeting-ats/presentation/batch/src/main/resources/logback/logback-text-console.xml (+0 -0)
- greeting-evaluation/adaptor/retrofit/build.gradle.kts (+2 -1)
- greeting-evaluation/adaptor/retrofit/src/main/kotlin/doodlin/greeting/evaluation/adaptor/retrofit/configuration/RetrofitClientConfiguration.kt (+5 -1)
- greeting-evaluation/adaptor/retrofit/src/main/kotlin/doodlin/greeting/evaluation/adaptor/retrofit/configuration/TracingOkHttpConfiguration.kt (+48 -0)
- greeting-evaluation/presentation/api/build.gradle.kts (+1 -0)
- greeting-evaluation/presentation/api/src/main/resources/logback-spring.xml (+4 -4)
- greeting-evaluation/presentation/api/src/main/resources/logback/logback-json-console.xml (+0 -0)
- greeting-evaluation/presentation/api/src/main/resources/logback/logback-text-console.xml (+0 -0)
- greeting-recruit-applicant/adaptor/kafka/build.gradle.kts (+1 -1)
- greeting-recruit-applicant/adaptor/kafka/src/main/resources/logback/logback-json-console.xml (+0 -0)
- greeting-recruit-applicant/adaptor/kafka/src/main/resources/logback/logback-text-console.xml (+0 -0)
- greeting-recruit-applicant/adaptor/retrofit/build.gradle.kts (+1 -0)
- greeting-recruit-applicant/adaptor/retrofit/src/main/kotlin/doodlin/greeting/recruit/applicant/adaptor/retrofit/configuration/TracingOkHttpConfiguration.kt (+48 -0)
- greeting-recruit-applicant/adaptor/retrofit/src/main/kotlin/doodlin/greeting/recruit/applicant/adaptor/retrofit/crypto/config/CryptoApiConfig.kt (+4 -2)
- greeting-recruit-applicant/adaptor/retrofit/src/main/kotlin/doodlin/greeting/recruit/applicant/adaptor/retrofit/normalized/config/NormalizedApiConfig.kt (+3 -1)
- greeting-recruit-applicant/adaptor/retrofit/src/main/kotlin/doodlin/greeting/recruit/applicant/adaptor/retrofit/opening/config/NewBackApiConfig.kt (+3 -1)
- greeting-recruit-applicant/adaptor/retrofit/src/main/kotlin/doodlin/greeting/recruit/applicant/adaptor/retrofit/opening/config/OpeningApiConfig.kt (+3 -1)
- greeting-recruit-applicant/adaptor/retrofit/src/main/kotlin/doodlin/greeting/recruit/applicant/adaptor/retrofit/workspace/config/WorkspaceApiConfig.kt (+3 -1)
- greeting-recruit-applicant/presentation/api/build.gradle.kts (+3 -1)
- greeting-recruit-applicant/presentation/api/src/main/kotlin/doodlin/greeting/recruit/applicant/AsyncTracingConfiguration.kt (+23 -0)
- greeting-recruit-applicant/presentation/api/src/main/resources/logback-spring.xml (+4 -4)
- greeting-recruit-applicant/presentation/api/src/main/resources/logback/logback-json-console.xml (+0 -0)
- greeting-recruit-applicant/presentation/api/src/main/resources/logback/logback-text-console.xml (+0 -0)
- greeting-recruit-applicant/presentation/api/src/test/kotlin/doodlin/greeting/recruit/applicant/AsyncTracingConfigurationTest.kt (+74 -0)
- greeting-recruit-applicant/presentation/batch/build.gradle.kts (+2 -1)
- greeting-recruit-applicant/presentation/batch/src/main/resources/logback-spring.xml (+4 -4)
- greeting-recruit-applicant/presentation/batch/src/main/resources/logback/logback-json-console.xml (+0 -0)
- greeting-recruit-applicant/presentation/batch/src/main/resources/logback/logback-text-console.xml (+0 -0)
- greeting-wno/adaptor/kafka/build.gradle.kts (+1 -1)
- greeting-wno/adaptor/kafka/src/main/resources/logback/logback-json-console.xml (+0 -0)
- greeting-wno/adaptor/kafka/src/main/resources/logback/logback-text-console.xml (+0 -0)
- greeting-wno/adaptor/retrofit/build.gradle.kts (+2 -1)
- greeting-wno/adaptor/retrofit/src/main/kotlin/doodlin/greeting/wno/adaptor/retrofit/authz/config/AuthzApiConfig.kt (+3 -1)
- greeting-wno/adaptor/retrofit/src/main/kotlin/doodlin/greeting/wno/adaptor/retrofit/config/TracingOkHttpConfiguration.kt (+48 -0)
- greeting-wno/presentation/api/build.gradle.kts (+2 -1)
- greeting-wno/presentation/api/src/main/resources/logback-spring.xml (+4 -4)
- greeting-wno/presentation/api/src/main/resources/logback/logback-json-console.xml (+0 -0)
- greeting-wno/presentation/api/src/main/resources/logback/logback-text-console.xml (+0 -0)

## 정적 분석 결과 (자동)
없음

## 리뷰 요청 사항
아래 관점으로 PR diff를 분석하고, 이 파일과 같은 디렉토리의 pr414.json 파일의 inlineComments에 결과를 추가해주세요.

1. **비즈니스 로직**: PRD/TDD AC 충족 여부, 빠진 엣지 케이스
2. **버그 우려**: NPE, 동시성, 리소스 누수, 에러 삼킴, 데이터 유실
3. **성능 우려**: N+1, 페이지네이션, 트랜잭션 내 외부 호출
4. **보안**: SQL Injection, 권한 누락, 시크릿 노출
5. **PRD/TDD 누락**: 명시된 기능 미구현, API 스펙 불일치

## 관련 컨텍스트 위치
- PR diff: `gh pr diff 414 --repo doodlincorp/greeting-ats`
- 정적 분석: 같은 디렉토리의 `pr414.json`
- .analysis/ 파이프라인 문서: 자동 매칭됨 (file-matcher)
