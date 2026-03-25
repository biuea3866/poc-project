# [GRT-4002] DB 스키마 마이그레이션 (신규 ERD + 기존 데이터 이관)

## 개요
- PRD: https://doodlin.atlassian.net/wiki/x/SICjdg
- Phase: 1 (서비스 구축)
- 예상 공수: 3d
- 의존성: GRT-4001

**범위:** alerts/alert_configs 기반 ERD → notifications 중심 신규 테이블 구조 생성 + 기존 데이터 이관 + 레거시 테이블 deprecated 처리

## 작업 내용

### 1. 신규 테이블 CREATE

#### notifications (alerts 대체)
```sql
CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL COMMENT 'EVALUATION_COMPLETED, EVALUATION_SUBMITTED, STAGE_ENTRY, INTERVIEW_REMIND, EVALUATION_REMIND, SYSTEM',
    notification_category VARCHAR(30) NOT NULL COMMENT 'EVALUATION, STAGE, REMIND, SYSTEM',
    channel VARCHAR(20) NOT NULL COMMENT 'IN_APP, EMAIL, SLACK',
    title VARCHAR(500) NOT NULL,
    content TEXT NULL,
    metadata JSON NULL COMMENT '알림별 추가 데이터 (applicantId, stageId 등)',
    source_type VARCHAR(50) NULL COMMENT 'APPLICANT, MEETING, EVALUATION',
    source_id BIGINT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    expire_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_noti_recipient (workspace_id, recipient_user_id, is_read, created_at DESC),
    INDEX idx_noti_type (workspace_id, notification_type, created_at DESC),
    INDEX idx_noti_source (source_type, source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### notification_settings (alert_configs 대체)
```sql
CREATE TABLE notification_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'IN_APP',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    config JSON NULL COMMENT '유형별 추가 설정 (target_role_ids, stage_ids 등)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uq_setting_ws_type_channel (workspace_id, notification_type, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### notification_subscriptions (신규)
```sql
CREATE TABLE notification_subscriptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'IN_APP',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    override_by_admin BOOLEAN NOT NULL DEFAULT FALSE COMMENT '관리자가 강제 설정한 값인지',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uq_sub_user_type_channel (workspace_id, user_id, notification_type, channel),
    INDEX idx_sub_user (workspace_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### notification_templates (신규)
```sql
CREATE TABLE notification_templates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NULL COMMENT 'NULL이면 시스템 기본 템플릿',
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    subject_template TEXT NULL,
    body_template TEXT NOT NULL,
    variables JSON NOT NULL COMMENT '사용 가능 변수 목록 [{name, description, required}]',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_template_ws_type (workspace_id, notification_type, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### notification_schedules (신규)
```sql
CREATE TABLE notification_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    target_id BIGINT NOT NULL COMMENT '면접ID 또는 평가ID',
    target_type VARCHAR(30) NOT NULL COMMENT 'MEETING, EVALUATION',
    scheduled_at TIMESTAMP NOT NULL COMMENT '발송 예정 시각',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, SENT, CANCELLED, FAILED',
    retry_count INT NOT NULL DEFAULT 0,
    max_retry INT NOT NULL DEFAULT 3,
    sent_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    cancel_reason VARCHAR(200) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_schedule_pending (status, scheduled_at),
    INDEX idx_schedule_target (target_type, target_id),
    INDEX idx_schedule_ws (workspace_id, notification_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### notification_logs (신규)
```sql
CREATE TABLE notification_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    notification_id BIGINT NULL,
    schedule_id BIGINT NULL,
    workspace_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL COMMENT 'SUCCESS, FAILED, SKIPPED',
    error_message TEXT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_log_noti (notification_id),
    INDEX idx_log_schedule (schedule_id),
    INDEX idx_log_ws_date (workspace_id, sent_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### consumed_events (멱등성 보장)
```sql
CREATE TABLE consumed_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(36) NOT NULL COMMENT 'UUID',
    topic VARCHAR(100) NOT NULL,
    consumed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uq_event_id (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2. 기존 데이터 마이그레이션

#### alerts -> notifications 이관
```sql
INSERT INTO notifications (workspace_id, recipient_user_id, notification_type, notification_category, channel, title, content, source_type, source_id, is_read, created_at)
SELECT
    a.workspace_id,
    a.user_id,
    CASE
        WHEN a.alert_type LIKE '%EVALUATION%' THEN 'EVALUATION_COMPLETED'
        WHEN a.alert_type LIKE '%STAGE%' THEN 'STAGE_ENTRY'
        WHEN a.alert_type LIKE '%MEETING%' THEN 'INTERVIEW_REMIND'
        ELSE 'SYSTEM'
    END,
    CASE
        WHEN a.alert_type LIKE '%EVALUATION%' THEN 'EVALUATION'
        WHEN a.alert_type LIKE '%STAGE%' THEN 'STAGE'
        WHEN a.alert_type LIKE '%MEETING%' THEN 'REMIND'
        ELSE 'SYSTEM'
    END,
    'IN_APP',
    a.title,
    a.content,
    NULL,
    NULL,
    a.is_read,
    a.created_at
FROM alerts a;
```

#### alert_configs -> notification_settings 이관
```sql
INSERT INTO notification_settings (workspace_id, notification_type, channel, enabled, created_at)
SELECT
    ac.workspace_id,
    ac.alert_type,
    'IN_APP',
    ac.enabled,
    COALESCE(ac.created_at, NOW())
FROM alert_configs ac;
```

### 3. 기존 테이블 RENAME _deprecated

```sql
RENAME TABLE alerts TO alerts_deprecated;
RENAME TABLE alert_configs TO alert_configs_deprecated;
```

> 주의: RENAME은 Phase 3 트래픽 전환 완료 후 실행. Phase 1에서는 신규 테이블만 생성.

### 4. 기본 템플릿 데이터 INSERT

```sql
INSERT INTO notification_templates (workspace_id, notification_type, channel, subject_template, body_template, variables, is_default) VALUES
(NULL, 'EVALUATION_COMPLETED', 'EMAIL', '${applicantName}님의 평가가 완료되었습니다', '${applicantName}님의 ${stageName} 전형 평가가 모두 완료되었습니다. 평가 결과를 확인해 주세요.', '[{"name":"applicantName","description":"지원자명","required":true},{"name":"stageName","description":"전형명","required":true}]', TRUE),
(NULL, 'INTERVIEW_REMIND', 'EMAIL', '면접 리마인드: ${applicantName}님 면접이 ${remainTime} 남았습니다', '${evaluatorName}님, ${applicantName}님과의 면접이 ${meetingDate} ${meetingTime}에 예정되어 있습니다.', '[{"name":"applicantName","description":"지원자명","required":true},{"name":"evaluatorName","description":"면접관명","required":true},{"name":"meetingDate","description":"면접일","required":true},{"name":"meetingTime","description":"면접시간","required":true},{"name":"remainTime","description":"남은시간","required":true}]', TRUE),
(NULL, 'EVALUATION_REMIND', 'EMAIL', '평가 리마인드: ${applicantName}님의 평가를 완료해 주세요', '${evaluatorName}님, ${applicantName}님의 ${stageName} 전형 평가가 아직 완료되지 않았습니다. 평가를 진행해 주세요.', '[{"name":"applicantName","description":"지원자명","required":true},{"name":"evaluatorName","description":"평가자명","required":true},{"name":"stageName","description":"전형명","required":true}]', TRUE);
```

### 수정 파일 목록

| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting-db-schema | flyway | V2026031700__create_notifications.sql | 신규 |
| greeting-db-schema | flyway | V2026031701__create_notification_settings.sql | 신규 |
| greeting-db-schema | flyway | V2026031702__create_notification_subscriptions.sql | 신규 |
| greeting-db-schema | flyway | V2026031703__create_notification_templates.sql | 신규 |
| greeting-db-schema | flyway | V2026031704__create_notification_schedules.sql | 신규 |
| greeting-db-schema | flyway | V2026031705__create_notification_logs.sql | 신규 |
| greeting-db-schema | flyway | V2026031706__create_consumed_events.sql | 신규 |
| greeting-db-schema | flyway | V2026031707__migrate_alerts_data.sql | 신규 |
| greeting-db-schema | flyway | V2026031708__migrate_alert_configs_data.sql | 신규 |
| greeting-db-schema | flyway | V2026031709__insert_default_templates.sql | 신규 |
| greeting-db-schema | flyway | V2026031710__rename_deprecated_tables.sql | 신규 (Phase 3) |

## 영향 범위

- greeting-new-back: alerts, alert_configs 직접 참조 코드 → Phase 3 RENAME 전까지 영향 없음
- greeting-notification-server (Node.js): alerts 테이블 읽기 → Phase 3까지 공존
- greeting-alert-server (Node.js): alerts 테이블 읽기 → Phase 3까지 공존
- greeting-communication: evaluation_remind_settings 참조 → Phase 2에서 전환

## 테스트 케이스

| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| TC-02-01 | 신규 테이블 생성 | 빈 DB | Flyway 마이그레이션 실행 | 7개 테이블 생성, 인덱스/제약조건 확인 |
| TC-02-02 | 데이터 마이그레이션 정합성 | alerts 100건, alert_configs 20건 존재 | 마이그레이션 스크립트 실행 | notifications 100건, notification_settings 20건, 데이터 매핑 정확 |
| TC-02-03 | 마이그레이션 롤백 | 마이그레이션 완료 상태 | 롤백 스크립트 실행 | 신규 테이블 DROP, 기존 테이블 정상 |
| TC-02-04 | 중복 마이그레이션 방지 | 마이그레이션 이미 실행됨 | 동일 마이그레이션 재실행 | Flyway가 skip, 에러 없음 |
| TC-02-05 | 유니크 제약조건 확인 | notification_settings 1건 존재 | 동일 (workspace_id, type, channel) INSERT | 유니크 제약조건 위반 에러 |
| TC-02-06 | 기본 템플릿 INSERT | 마이그레이션 완료 | notification_templates 조회 | 3개 기본 템플릿 존재, is_default=TRUE |

## 기대 결과 (AC)

- [ ] Flyway 마이그레이션 전체 성공 (V2026031700 ~ V2026031709)
- [ ] 7개 테이블 생성 확인 (notifications, notification_settings, notification_subscriptions, notification_templates, notification_schedules, notification_logs, consumed_events)
- [ ] alerts → notifications 데이터 마이그레이션 정합성 (건수 일치, 필드 매핑 정확)
- [ ] alert_configs → notification_settings 데이터 마이그레이션 정합성
- [ ] 기본 템플릿 3종 INSERT 확인
- [ ] 롤백 스크립트 준비 완료
- [ ] 기존 alerts, alert_configs 테이블 정상 유지 (Phase 3 전까지 RENAME 안 함)

## 체크리스트

- [ ] Flyway 버전 넘버링 기존 스키마와 충돌 없는지 확인
- [ ] dev 환경에서 dry-run 실행 후 데이터 검증
- [ ] 대용량 테이블(alerts) 마이그레이션 시 lock 시간 고려 (pt-online-schema-change 검토)
- [ ] 롤백 스크립트 별도 작성 (R__rollback_notification_tables.sql)
- [ ] DBA 리뷰 요청
