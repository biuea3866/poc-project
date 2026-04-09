-- ============================================================
-- V3: Point 도메인을 closet-promotion에서 closet-member로 이관
-- Point(적립금)는 프로모션이 아닌 회원의 금융 자산이므로 member BC로 이동
-- ============================================================

-- 기존 point_history 테이블 스키마를 PointBalance 기반 구조로 변경
-- (기존 V1에서 생성된 point_history는 simplified 버전이었음)

-- 기존 point_history 테이블 삭제 후 재생성 (PointBalance 연동 구조)
DROP TABLE IF EXISTS point_history;

CREATE TABLE point_history (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '적립금 이력 ID',
    member_id           BIGINT          NOT NULL                 COMMENT '회원 ID',
    amount              INT             NOT NULL                 COMMENT '변동 금액 (양수: 적립, 음수: 차감)',
    balance_after       INT             NOT NULL                 COMMENT '변동 후 잔액',
    transaction_type    VARCHAR(30)     NOT NULL                 COMMENT '거래 유형 (EARN/USE/CANCEL_EARN/CANCEL_USE/EXPIRE)',
    reference_type      VARCHAR(30)     NULL                     COMMENT '참조 유형 (ORDER/REVIEW/EVENT)',
    reference_id        BIGINT          NULL                     COMMENT '참조 ID',
    expired_at          DATETIME(6)     NULL                     COMMENT '만료일시',
    created_at          DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    PRIMARY KEY (id),
    INDEX idx_point_history_member (member_id, created_at DESC),
    INDEX idx_point_history_reference (reference_type, reference_id),
    INDEX idx_point_history_transaction (transaction_type),
    INDEX idx_point_history_expired (expired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='적립금 이력';

-- Point Balance (적립금 잔액) — 회원별 적립금 원장
CREATE TABLE point_balance (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '적립금 잔액 ID',
    member_id           BIGINT          NOT NULL                 COMMENT '회원 ID',
    total_points        INT             NOT NULL DEFAULT 0       COMMENT '총 적립금',
    available_points    INT             NOT NULL DEFAULT 0       COMMENT '사용 가능 적립금',
    created_at          DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    updated_at          DATETIME(6)     NOT NULL                 COMMENT '수정일시',
    PRIMARY KEY (id),
    UNIQUE INDEX ux_point_balance_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='적립금 잔액';

-- Point Policy (적립금 정책)
-- grade_type이 NULL이면 전체 등급 적용 (기존 GradeType.ALL 대체)
CREATE TABLE point_policy (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '적립금 정책 ID',
    event_type          VARCHAR(30)     NOT NULL                 COMMENT '이벤트 유형 (PURCHASE/REVIEW/PHOTO_REVIEW/SIGN_UP)',
    grade_type          VARCHAR(30)     NULL                     COMMENT '회원 등급 (NULL=전체, NORMAL/SILVER/GOLD/PLATINUM)',
    point_amount        INT             NULL                     COMMENT '정액 적립금',
    point_rate          DECIMAL(5,2)    NULL                     COMMENT '정률 적립율 (%)',
    description         VARCHAR(500)    NOT NULL                 COMMENT '정책 설명',
    is_active           TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '활성 여부',
    created_at          DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    updated_at          DATETIME(6)     NOT NULL                 COMMENT '수정일시',
    PRIMARY KEY (id),
    INDEX idx_point_policy_event (event_type, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='적립금 정책';
