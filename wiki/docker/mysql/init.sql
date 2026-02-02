CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `email` VARCHAR(255) NOT NULL UNIQUE COMMENT '로그인 email',
    `password` VARCHAR(255) NOT NULL COMMENT '암호화된 비밀번호',
    `name` VARCHAR(100) NOT NULL COMMENT '유저 이름',
    `created_at` DATETIME NOT NULL COMMENT '생성 일시',
    `updated_at` DATETIME NOT NULL COMMENT '수정 일시',
    `deleted_at` DATETIME NULL COMMENT '삭제 일시',
    INDEX `idx_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL COMMENT '제목',
    `content` TEXT NULL COMMENT '글 내용',
    `status` VARCHAR(20) NOT NULL COMMENT '상태(PENDING, COMPLETED, FAILED, DELETED)',
    `parent_id` BIGINT NULL,
    `created_at` DATETIME NOT NULL COMMENT '생성 일시',
    `updated_at` DATETIME NOT NULL COMMENT '수정 일시',
    `deleted_at` DATETIME NULL COMMENT '삭제 일시',
    `created_by` BIGINT NOT NULL COMMENT '생성 유저 id',
    `updated_by` BIGINT NOT NULL COMMENT '수정 유저 Id',
    INDEX `idx_document_parent_id` (`parent_id`),
    INDEX `idx_document_id_created_at` (`id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `document_revision` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `data` TEXT NOT NULL COMMENT 'document 전체 정보 json',
    `document_id` BIGINT NOT NULL COMMENT '글 id',
    `created_at` DATETIME NOT NULL COMMENT '생성 일시',
    `created_by` BIGINT NOT NULL COMMENT '생성 유저 id',
    INDEX `idx_document_revision_document_id` (`document_id`),
    INDEX `idx_document_revision_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tag_type` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `tag_type` VARCHAR(100) NOT NULL COMMENT '태그 타입 (enum 문자열)',
    `created_at` DATETIME NOT NULL COMMENT '생성 일시',
    `updated_at` DATETIME NOT NULL COMMENT '수정 일시',
    `deleted_at` DATETIME NULL COMMENT '삭제 일시'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL COMMENT '전역 태그 이름',
    `tag_type_id` BIGINT NOT NULL COMMENT '태그 타입 id',
    `created_at` DATETIME NOT NULL COMMENT '생성 일시',
    `updated_at` DATETIME NOT NULL COMMENT '수정 일시',
    `deleted_at` DATETIME NULL COMMENT '삭제 일시'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tag_document_mapping` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `tag_id` BIGINT NOT NULL COMMENT 'tag id',
    `document_id` BIGINT NOT NULL COMMENT '글 id',
    `document_revision_id` BIGINT NOT NULL COMMENT '글 개정 정보 id',
    `created_at` DATETIME NOT NULL COMMENT '생성 일시'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `document_summary` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `content` TEXT NOT NULL COMMENT 'ai 요약 정보',
    `document_revision_id` BIGINT NOT NULL COMMENT '글 개정 정보 id',
    `document_id` BIGINT NOT NULL COMMENT '글 id',
    INDEX `idx_document_summary_doc_rev` (`document_id`, `document_revision_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ai_agent_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `agent_type` VARCHAR(50) NOT NULL COMMENT '에이전트 타입 (SUMMARY, TAGGER, WEB_SEARCH, RAG)',
    `status` VARCHAR(30) NOT NULL COMMENT '성공 / 실패 여부(SUCCESS, FAILURE)',
    `action_detail` TEXT NOT NULL COMMENT '수행 내용 (예: 외부 웹 검색 수행)',
    `reference_data` TEXT NULL COMMENT '참고한 url 목록이나 원본 소스',
    `document_revision_id` BIGINT NOT NULL COMMENT '글 개정 정보 id',
    `document_id` BIGINT NOT NULL COMMENT '글 id',
    `executor_id` BIGINT NOT NULL COMMENT '요청 user id',
    `created_at` DATETIME NOT NULL COMMENT '생성 시점',
    INDEX `idx_ai_agent_log_doc_rev` (`document_id`, `document_revision_id`),
    INDEX `idx_ai_agent_log_executor_id` (`executor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
