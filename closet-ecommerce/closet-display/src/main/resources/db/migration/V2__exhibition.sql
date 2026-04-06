-- Display 도메인 테이블 (CP-48: 기획전/컬렉션 관리)

CREATE TABLE exhibition (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '기획전 ID',
    title           VARCHAR(200)    NOT NULL COMMENT '기획전 제목',
    description     VARCHAR(1000)   NOT NULL COMMENT '기획전 설명',
    image_url       VARCHAR(500)    NOT NULL COMMENT '기획전 대표 이미지 URL',
    started_at      DATETIME(6)     NOT NULL COMMENT '기획전 시작일시',
    ended_at        DATETIME(6)     NOT NULL COMMENT '기획전 종료일시',
    status          VARCHAR(20)     NOT NULL COMMENT '기획전 상태 (SCHEDULED/ACTIVE/ENDED)',
    sort_order      INT             NOT NULL COMMENT '정렬 순서 (1부터 시작, 오름차순)',
    created_at      DATETIME(6)     NOT NULL COMMENT '생성일시',
    updated_at      DATETIME(6)     NOT NULL COMMENT '수정일시',
    PRIMARY KEY (id),
    INDEX idx_exhibition_status (status),
    INDEX idx_exhibition_status_ended (status, ended_at),
    INDEX idx_exhibition_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='기획전';

CREATE TABLE exhibition_product (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '기획전 상품 ID',
    exhibition_id   BIGINT          NOT NULL COMMENT '기획전 ID',
    product_id      BIGINT          NOT NULL COMMENT '상품 ID',
    sort_order      INT             NOT NULL COMMENT '정렬 순서 (1부터 시작, 오름차순)',
    added_at        DATETIME(6)     NOT NULL COMMENT '상품 추가일시',
    PRIMARY KEY (id),
    INDEX idx_exhibition_product_exhibition (exhibition_id),
    INDEX idx_exhibition_product_exhibition_sort (exhibition_id, sort_order),
    INDEX idx_exhibition_product_product (product_id),
    UNIQUE INDEX ux_exhibition_product (exhibition_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='기획전 상품';
