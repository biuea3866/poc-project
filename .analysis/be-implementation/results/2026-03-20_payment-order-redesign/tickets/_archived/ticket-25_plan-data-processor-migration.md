# [Ticket #25] plan-data-processor → greeting-new-back 이관

## 개요
- TDD 참조: tdd.md 섹션 5.1 (greeting_plan-data-processor → greeting-new-back 이관), 8.4 (plan-data-processor 설계 결정)
- 선행 티켓: #21, #24
- 크기: L

## 작업 내용

### 변경 사항

#### 1. 현재 상태 (AS-IS)
- `greeting_plan-data-processor`는 Node.js/NestJS로 구현된 Kafka Consumer 서비스
- 소비 토픽: `event.ats.plan.changed.v1`, `basic-plan.changed`, `standard-plan.changed`
- 역할: 플랜 변경(특히 다운그레이드) 시 ATS 도메인 데이터 정리
  - Standard → Basic 다운그레이드: 평가(evaluation), 템플릿(template), 커스텀 설정 제거
  - Basic → Free 다운그레이드: 추가로 이메일 폼(email form) 제거
- 문제: ATS 도메인 데이터를 외부 서비스(plan-data-processor)가 관리하는 것은 도메인 경계 위반

#### 2. PlanDowngradeEventHandler 구현 (greeting-new-back)
- `subscription.changed.v1` 토픽을 소비하는 전용 Handler
- changeType=DOWNGRADE 이벤트만 처리 (다른 changeType은 무시)
- Consumer Group: `greeting-new-back-plan-downgrade-handler` (#24의 PlanOnWorkspace 동기화와 별도 Consumer Group)

#### 3. 다운그레이드 로직 이식 (Node.js → Kotlin)

##### Standard → Basic 다운그레이드 시:
1. **평가(Evaluation) 정리**
   - Standard 전용 평가 항목 비활성화 또는 삭제(soft delete)
   - 해당 workspace의 Standard 전용 evaluation setting 초기화
2. **템플릿(Template) 정리**
   - Standard 전용 템플릿 비활성화
   - 커스텀 템플릿 중 Standard 기능 의존 항목 비활성화
3. **커스텀 설정(Custom Settings) 정리**
   - Standard 전용 opening 설정 초기화 (기본값 복원)
   - 고급 필터/자동화 설정 비활성화

##### Basic → Free 다운그레이드 시 (위 항목 포함 + 추가):
4. **이메일 폼(Email Form) 정리**
   - 커스텀 이메일 폼 비활성화
   - 기본 이메일 폼으로 복원

#### 4. 멱등성 보장
- 모든 기능 제거/비활성화 로직은 멱등(idempotent)
  - 이미 비활성화된 항목 재비활성화 → 정상 처리 (에러 아님)
  - 이미 삭제된(soft delete) 항목 재삭제 → 무시
- 동일 이벤트 재수신 시 동일 결과

#### 5. 기존 plan-data-processor 토픽 병행 소비
- 이관 기간 동안 plan-data-processor와 greeting-new-back 모두 소비
  - plan-data-processor: 기존 토픽(event.ats.plan.changed.v1, basic-plan.changed, standard-plan.changed) 소비 유지
  - greeting-new-back: subscription.changed.v1 소비
- Feature flag: `payment.plan-downgrade-handler.enabled=true`
- 이관 완료 확인 후 plan-data-processor 중지 (#26에서 처리)

#### 6. 로직 상세 (Node.js 코드 기반 포팅)

```
// 다운그레이드 처리 의사코드
fun handleDowngrade(event: SubscriptionChangedEvent) {
    val previousLevel = getPlanLevel(event.previousPlanCode)  // STANDARD=3, BASIC=2, FREE=1
    val newLevel = getPlanLevel(event.planCode)

    if (previousLevel <= newLevel) return  // 업그레이드이면 무시

    val workspaceId = event.workspaceId

    // Standard → Basic 또는 Standard → Free
    if (previousLevel >= STANDARD && newLevel < STANDARD) {
        evaluationService.deactivateStandardFeatures(workspaceId)
        templateService.deactivateStandardTemplates(workspaceId)
        openingSettingService.resetStandardSettings(workspaceId)
    }

    // Basic → Free (또는 Standard → Free)
    if (previousLevel >= BASIC && newLevel < BASIC) {
        emailFormService.deactivateCustomEmailForms(workspaceId)
    }
}
```

#### 7. 대상 엔티티/서비스 (greeting-new-back 기존 코드 활용)
- greeting-new-back이 이미 접근 가능한 엔티티:
  - Evaluation / EvaluationSetting
  - OpeningSetting / Opening
  - Template
  - EmailForm
- 기존 Service/Repository를 활용하여 비활성화 메서드 추가

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting-new-back | application | application/plan/PlanDowngradeEventHandler.kt | 신규 |
| greeting-new-back | application | application/plan/PlanDowngradeService.kt | 신규 (다운그레이드 로직 오케스트레이션) |
| greeting-new-back | infrastructure | infrastructure/kafka/consumer/PlanDowngradeConsumer.kt | 신규 |
| greeting-new-back | application | application/evaluation/EvaluationService.kt | 수정 (deactivateStandardFeatures 메서드 추가) |
| greeting-new-back | application | application/template/TemplateService.kt | 수정 (deactivateStandardTemplates 메서드 추가) |
| greeting-new-back | application | application/opening/OpeningSettingService.kt | 수정 (resetStandardSettings 메서드 추가) |
| greeting-new-back | application | application/email/EmailFormService.kt | 수정 (deactivateCustomEmailForms 메서드 추가) |
| greeting-new-back | domain | domain/evaluation/Evaluation.kt | 수정 (비활성화 메서드 추가, 필요 시) |
| greeting-new-back | domain | domain/template/Template.kt | 수정 (비활성화 메서드 추가, 필요 시) |
| greeting-new-back | domain | domain/email/EmailForm.kt | 수정 (비활성화 메서드 추가, 필요 시) |
| greeting-new-back | infrastructure | infrastructure/config/KafkaConsumerConfig.kt | 수정 (PlanDowngrade Consumer Group 추가) |
| greeting-new-back | resources | application.yml | 수정 (feature flag 추가) |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T25-01 | Standard → Basic 다운그레이드 | workspace에 Standard 전용 평가/템플릿/설정 존재 | subscription.changed.v1 (DOWNGRADE, PLAN_STANDARD → PLAN_BASIC) | 평가 비활성화, 템플릿 비활성화, 커스텀 설정 초기화 |
| T25-02 | Standard → Free 다운그레이드 | workspace에 Standard+Basic 기능 모두 존재 | subscription.changed.v1 (DOWNGRADE, PLAN_STANDARD → PLAN_FREE) | Standard 기능 + 이메일 폼 모두 비활성화 |
| T25-03 | Basic → Free 다운그레이드 | workspace에 커스텀 이메일 폼 존재 | subscription.changed.v1 (DOWNGRADE, PLAN_BASIC → PLAN_FREE) | 커스텀 이메일 폼 비활성화 |
| T25-04 | 멱등성 - 이미 비활성화된 항목 | 모든 Standard 기능 이미 비활성화 | 동일 다운그레이드 이벤트 재수신 | 에러 없이 정상 처리, 상태 변화 없음 |
| T25-05 | 업그레이드 이벤트 무시 | changeType=UPGRADE | subscription.changed.v1 (UPGRADE) | 처리 스킵, 로그만 기록 |
| T25-06 | RENEWAL 이벤트 무시 | changeType=RENEWAL | subscription.changed.v1 (RENEWAL) | 처리 스킵 |
| T25-07 | Feature flag 비활성화 | payment.plan-downgrade-handler.enabled=false | 다운그레이드 이벤트 수신 | Consumer 미동작 |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T25-E01 | 존재하지 않는 workspace | workspaceId에 해당 workspace 없음 | 다운그레이드 이벤트 수신 | 경고 로그 기록, 에러 발생 안 함 (무시) |
| T25-E02 | 평가 비활성화 중 부분 실패 | 5건 중 3건째에서 DB 에러 | 다운그레이드 처리 중 | 트랜잭션 롤백 → 재시도 → 전체 성공 또는 DLQ |
| T25-E03 | previousPlanCode 없음 | previousPlanCode=null | 다운그레이드 이벤트 수신 | planCode 기준으로 현재 레벨 이하 기능만 유지하도록 처리 |
| T25-E04 | 대량 데이터 workspace | 평가 1000건, 템플릿 500건 | 다운그레이드 처리 | 배치 처리로 성능 보장 (bulk update) |
| T25-E05 | 동시 다운그레이드+데이터 수정 | 사용자가 평가 수정 중 다운그레이드 | 동시 처리 | 비활성화 우선, 사용자 수정은 비활성화된 상태에서 실패 또는 무시 |

## 기대 결과 (AC)
- [ ] greeting-new-back에 PlanDowngradeEventHandler가 구현되어 subscription.changed.v1의 DOWNGRADE 이벤트를 처리한다
- [ ] Standard → Basic 다운그레이드 시 평가, 템플릿, 커스텀 설정이 비활성화/초기화된다
- [ ] Basic → Free 다운그레이드 시 추가로 커스텀 이메일 폼이 비활성화된다
- [ ] 모든 기능 제거 로직이 멱등하여 중복 이벤트 수신 시에도 안전하다
- [ ] 기존 plan-data-processor와 병행 운영이 가능하다 (feature flag 제어)
- [ ] greeting-new-back의 기존 엔티티/서비스를 활용하여 도메인 경계가 올바르게 설정된다
- [ ] DOWNGRADE 외의 changeType 이벤트는 무시된다
