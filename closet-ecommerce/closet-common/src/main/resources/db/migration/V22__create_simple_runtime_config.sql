-- Simple Runtime Config (런타임 Feature Flag / 설정)
CREATE TABLE simple_runtime_config (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '런타임 설정 ID',
    config_key      VARCHAR(100)    NOT NULL                 COMMENT '설정 키 (UNIQUE)',
    config_value    VARCHAR(500)    NOT NULL                 COMMENT '설정 값',
    description     VARCHAR(500)    NULL                     COMMENT '설정 설명',
    created_at      DATETIME(6)     NOT NULL                 COMMENT '생성일시',
    updated_at      DATETIME(6)     NOT NULL                 COMMENT '수정일시',
    deleted_at      DATETIME(6)     NULL                     COMMENT '삭제일시',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='런타임 설정 (Feature Flag)';
