package com.hrplatform.auth.migration

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.flywaydb.core.Flyway
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.Connection
import java.sql.DriverManager

/**
 * Flyway V1 마이그레이션 순차 적용 검증.
 *
 * Testcontainers MySQL 8.0 컨테이너를 직접 구동하여 Spring 컨텍스트 의존 없이 DDL만 검증한다.
 * 검증 항목:
 *   1) 7개 테이블 생성 여부
 *   2) 각 테이블 인덱스 정확성 (UNIQUE 포함)
 *   3) 주요 컬럼의 NOT NULL / DEFAULT / DATA_TYPE 명세 준수
 *   4) 모든 테이블에 COMMENT 적용 여부
 *   5) Flyway 체크섬 — V1 success=1
 */
class FlywayMigrationTest : BehaviorSpec({

    val mysql: MySQLContainer<*> = MySQLContainer(DockerImageName.parse("mysql:8.0"))
        .withDatabaseName("auth_db")
        .withUsername("test")
        .withPassword("test")
        .withCommand(
            "--character-set-server=utf8mb4",
            "--collation-server=utf8mb4_unicode_ci",
        )

    beforeSpec { mysql.start() }
    afterSpec { mysql.stop() }

    given("Flyway V1 마이그레이션을 적용하면") {

        val flyway = Flyway.configure()
            .dataSource(mysql.jdbcUrl, mysql.username, mysql.password)
            .locations("classpath:db/migration")
            .load()

        flyway.migrate()

        val connection: Connection = DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password)

        // ─────────────────────────────────────────────────────────────────
        // 공통 헬퍼
        // ─────────────────────────────────────────────────────────────────
        fun isNullable(table: String, column: String): Boolean {
            connection.prepareStatement(
                "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'auth_db' AND TABLE_NAME = ? AND COLUMN_NAME = ?",
            ).use { ps ->
                ps.setString(1, table)
                ps.setString(2, column)
                ps.executeQuery().use { rs ->
                    rs.next()
                    return rs.getString(1) == "YES"
                }
            }
        }

        fun dataType(table: String, column: String): String {
            connection.prepareStatement(
                "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'auth_db' AND TABLE_NAME = ? AND COLUMN_NAME = ?",
            ).use { ps ->
                ps.setString(1, table)
                ps.setString(2, column)
                ps.executeQuery().use { rs ->
                    rs.next()
                    return rs.getString(1)
                }
            }
        }

        fun columnDefault(table: String, column: String): String? {
            connection.prepareStatement(
                "SELECT COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'auth_db' AND TABLE_NAME = ? AND COLUMN_NAME = ?",
            ).use { ps ->
                ps.setString(1, table)
                ps.setString(2, column)
                ps.executeQuery().use { rs ->
                    rs.next()
                    return rs.getString(1)
                }
            }
        }

        fun hasIndex(table: String, indexName: String): Boolean {
            connection.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                    "WHERE TABLE_SCHEMA = 'auth_db' AND TABLE_NAME = ? AND INDEX_NAME = ?",
            ).use { ps ->
                ps.setString(1, table)
                ps.setString(2, indexName)
                ps.executeQuery().use { rs ->
                    rs.next()
                    return rs.getInt(1) > 0
                }
            }
        }

        fun isUniqueIndex(table: String, indexName: String): Boolean {
            connection.prepareStatement(
                "SELECT NON_UNIQUE FROM INFORMATION_SCHEMA.STATISTICS " +
                    "WHERE TABLE_SCHEMA = 'auth_db' AND TABLE_NAME = ? AND INDEX_NAME = ? LIMIT 1",
            ).use { ps ->
                ps.setString(1, table)
                ps.setString(2, indexName)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) return false
                    return rs.getInt(1) == 0
                }
            }
        }

        fun tableComment(table: String): String {
            connection.prepareStatement(
                "SELECT TABLE_COMMENT FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE TABLE_SCHEMA = 'auth_db' AND TABLE_NAME = ?",
            ).use { ps ->
                ps.setString(1, table)
                ps.executeQuery().use { rs ->
                    rs.next()
                    return rs.getString(1) ?: ""
                }
            }
        }

        fun columnComment(table: String, column: String): String {
            connection.prepareStatement(
                "SELECT COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'auth_db' AND TABLE_NAME = ? AND COLUMN_NAME = ?",
            ).use { ps ->
                ps.setString(1, table)
                ps.setString(2, column)
                ps.executeQuery().use { rs ->
                    rs.next()
                    return rs.getString(1) ?: ""
                }
            }
        }

        fun columnCharLength(table: String, column: String): Int? {
            connection.prepareStatement(
                "SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'auth_db' AND TABLE_NAME = ? AND COLUMN_NAME = ?",
            ).use { ps ->
                ps.setString(1, table)
                ps.setString(2, column)
                ps.executeQuery().use { rs ->
                    rs.next()
                    return rs.getInt(1).takeIf { rs.wasNull().not() }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // 1. 7개 테이블 존재 확인
        // ─────────────────────────────────────────────────────────────────
        `when`("SHOW TABLES 를 실행하면") {
            val tables = mutableListOf<String>()
            connection.createStatement().use { stmt ->
                stmt.executeQuery("SHOW TABLES").use { rs ->
                    while (rs.next()) tables.add(rs.getString(1).lowercase())
                }
            }

            then("7개 테이블이 모두 존재한다") {
                tables shouldContainAll listOf(
                    "user_account",
                    "role",
                    "user_account_role",
                    "refresh_token",
                    "login_attempt",
                    "two_factor_backup_code",
                    "api_token",
                )
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // 2. user_account 테이블 검증
        // ─────────────────────────────────────────────────────────────────
        `when`("user_account 테이블의 컬럼 명세를 확인하면") {
            then("id 는 NOT NULL BIGINT 이다") {
                isNullable("user_account", "id") shouldBe false
                dataType("user_account", "id") shouldBe "bigint"
            }

            then("employment_id 는 NOT NULL BIGINT 이다") {
                isNullable("user_account", "employment_id") shouldBe false
                dataType("user_account", "employment_id") shouldBe "bigint"
            }

            then("company_id 는 NOT NULL BIGINT 이다") {
                isNullable("user_account", "company_id") shouldBe false
                dataType("user_account", "company_id") shouldBe "bigint"
            }

            then("email 은 NOT NULL VARCHAR 이다") {
                isNullable("user_account", "email") shouldBe false
                dataType("user_account", "email") shouldBe "varchar"
            }

            then("password_hash 는 NOT NULL VARCHAR(60) 이다") {
                isNullable("user_account", "password_hash") shouldBe false
                dataType("user_account", "password_hash") shouldBe "varchar"
                columnCharLength("user_account", "password_hash") shouldBe 60
            }

            then("status 는 NOT NULL VARCHAR 이다") {
                isNullable("user_account", "status") shouldBe false
                dataType("user_account", "status") shouldBe "varchar"
            }

            then("failed_login_attempts 는 NOT NULL INT DEFAULT 0 이다") {
                isNullable("user_account", "failed_login_attempts") shouldBe false
                dataType("user_account", "failed_login_attempts") shouldBe "int"
                columnDefault("user_account", "failed_login_attempts") shouldBe "0"
            }

            then("locked_until 은 NULL 허용 TIMESTAMP 이다") {
                isNullable("user_account", "locked_until") shouldBe true
                dataType("user_account", "locked_until") shouldBe "timestamp"
            }

            then("last_login_at 은 NULL 허용 TIMESTAMP 이다") {
                isNullable("user_account", "last_login_at") shouldBe true
                dataType("user_account", "last_login_at") shouldBe "timestamp"
            }

            then("two_factor_enabled 는 NOT NULL TINYINT DEFAULT 0 이다") {
                isNullable("user_account", "two_factor_enabled") shouldBe false
                dataType("user_account", "two_factor_enabled") shouldBe "tinyint"
                columnDefault("user_account", "two_factor_enabled") shouldBe "0"
            }

            then("two_factor_secret 은 NULL 허용 VARBINARY 이다") {
                isNullable("user_account", "two_factor_secret") shouldBe true
                dataType("user_account", "two_factor_secret") shouldBe "varbinary"
            }

            then("created_at 은 NOT NULL TIMESTAMP 이다") {
                isNullable("user_account", "created_at") shouldBe false
                dataType("user_account", "created_at") shouldBe "timestamp"
            }

            then("updated_at 은 NOT NULL TIMESTAMP 이다") {
                isNullable("user_account", "updated_at") shouldBe false
                dataType("user_account", "updated_at") shouldBe "timestamp"
            }

            then("deleted_at 은 NULL 허용 TIMESTAMP 이다") {
                isNullable("user_account", "deleted_at") shouldBe true
                dataType("user_account", "deleted_at") shouldBe "timestamp"
            }
        }

        `when`("user_account 테이블의 인덱스를 확인하면") {
            then("uq_user_account_employment_id UNIQUE 인덱스가 존재한다") {
                hasIndex("user_account", "uq_user_account_employment_id") shouldBe true
                isUniqueIndex("user_account", "uq_user_account_employment_id") shouldBe true
            }

            then("uq_user_account_email UNIQUE 인덱스가 존재한다") {
                hasIndex("user_account", "uq_user_account_email") shouldBe true
                isUniqueIndex("user_account", "uq_user_account_email") shouldBe true
            }

            then("idx_user_account_company_id 인덱스가 존재한다") {
                hasIndex("user_account", "idx_user_account_company_id") shouldBe true
            }

            then("idx_user_account_status 인덱스가 존재한다") {
                hasIndex("user_account", "idx_user_account_status") shouldBe true
            }

            then("idx_user_account_deleted_at 인덱스가 존재한다") {
                hasIndex("user_account", "idx_user_account_deleted_at") shouldBe true
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // 3. role 테이블 검증
        // ─────────────────────────────────────────────────────────────────
        `when`("role 테이블의 컬럼 명세를 확인하면") {
            then("company_id 는 NOT NULL BIGINT 이다") {
                isNullable("role", "company_id") shouldBe false
                dataType("role", "company_id") shouldBe "bigint"
            }

            then("code 는 NOT NULL VARCHAR 이다") {
                isNullable("role", "code") shouldBe false
                dataType("role", "code") shouldBe "varchar"
            }

            then("name 은 NOT NULL VARCHAR 이다") {
                isNullable("role", "name") shouldBe false
                dataType("role", "name") shouldBe "varchar"
            }

            then("description 은 NULL 허용 VARCHAR 이다") {
                isNullable("role", "description") shouldBe true
                dataType("role", "description") shouldBe "varchar"
            }

            then("is_system_role 은 NOT NULL TINYINT DEFAULT 0 이다") {
                isNullable("role", "is_system_role") shouldBe false
                dataType("role", "is_system_role") shouldBe "tinyint"
                columnDefault("role", "is_system_role") shouldBe "0"
            }

            then("deleted_at 은 NULL 허용 TIMESTAMP 이다") {
                isNullable("role", "deleted_at") shouldBe true
                dataType("role", "deleted_at") shouldBe "timestamp"
            }
        }

        `when`("role 테이블의 인덱스를 확인하면") {
            then("uq_role_company_code UNIQUE 인덱스가 존재한다") {
                hasIndex("role", "uq_role_company_code") shouldBe true
                isUniqueIndex("role", "uq_role_company_code") shouldBe true
            }

            then("idx_role_deleted_at 인덱스가 존재한다") {
                hasIndex("role", "idx_role_deleted_at") shouldBe true
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // 4. user_account_role 테이블 검증
        // ─────────────────────────────────────────────────────────────────
        `when`("user_account_role 테이블의 컬럼 명세를 확인하면") {
            then("user_account_id 는 NOT NULL BIGINT 이다") {
                isNullable("user_account_role", "user_account_id") shouldBe false
                dataType("user_account_role", "user_account_id") shouldBe "bigint"
            }

            then("role_id 는 NOT NULL BIGINT 이다") {
                isNullable("user_account_role", "role_id") shouldBe false
                dataType("user_account_role", "role_id") shouldBe "bigint"
            }

            then("assigned_at 은 NOT NULL TIMESTAMP 이다") {
                isNullable("user_account_role", "assigned_at") shouldBe false
                dataType("user_account_role", "assigned_at") shouldBe "timestamp"
            }

            then("assigned_by 는 NULL 허용 BIGINT 이다") {
                isNullable("user_account_role", "assigned_by") shouldBe true
                dataType("user_account_role", "assigned_by") shouldBe "bigint"
            }
        }

        `when`("user_account_role 테이블의 인덱스를 확인하면") {
            then("uq_user_account_role_mapping UNIQUE 인덱스가 존재한다") {
                hasIndex("user_account_role", "uq_user_account_role_mapping") shouldBe true
                isUniqueIndex("user_account_role", "uq_user_account_role_mapping") shouldBe true
            }

            then("idx_user_account_role_account 인덱스가 존재한다") {
                hasIndex("user_account_role", "idx_user_account_role_account") shouldBe true
            }

            then("idx_user_account_role_role 인덱스가 존재한다") {
                hasIndex("user_account_role", "idx_user_account_role_role") shouldBe true
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // 5. refresh_token 테이블 검증
        // ─────────────────────────────────────────────────────────────────
        `when`("refresh_token 테이블의 컬럼 명세를 확인하면") {
            then("token_hash 는 NOT NULL CHAR(64) 이다") {
                isNullable("refresh_token", "token_hash") shouldBe false
                dataType("refresh_token", "token_hash") shouldBe "char"
                columnCharLength("refresh_token", "token_hash") shouldBe 64
            }

            then("expires_at 은 NOT NULL TIMESTAMP 이다") {
                isNullable("refresh_token", "expires_at") shouldBe false
                dataType("refresh_token", "expires_at") shouldBe "timestamp"
            }

            then("revoked_at 은 NULL 허용 TIMESTAMP 이다") {
                isNullable("refresh_token", "revoked_at") shouldBe true
                dataType("refresh_token", "revoked_at") shouldBe "timestamp"
            }

            then("ip_address 는 NULL 허용 VARCHAR(45) 이다") {
                isNullable("refresh_token", "ip_address") shouldBe true
                dataType("refresh_token", "ip_address") shouldBe "varchar"
                columnCharLength("refresh_token", "ip_address") shouldBe 45
            }
        }

        `when`("refresh_token 테이블의 인덱스를 확인하면") {
            then("uq_refresh_token_hash UNIQUE 인덱스가 존재한다") {
                hasIndex("refresh_token", "uq_refresh_token_hash") shouldBe true
                isUniqueIndex("refresh_token", "uq_refresh_token_hash") shouldBe true
            }

            then("idx_refresh_token_account 인덱스가 존재한다") {
                hasIndex("refresh_token", "idx_refresh_token_account") shouldBe true
            }

            then("idx_refresh_token_expires_at 인덱스가 존재한다") {
                hasIndex("refresh_token", "idx_refresh_token_expires_at") shouldBe true
            }

            then("idx_refresh_token_deleted_at 인덱스가 존재한다") {
                hasIndex("refresh_token", "idx_refresh_token_deleted_at") shouldBe true
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // 6. login_attempt 테이블 검증 (append-only)
        // ─────────────────────────────────────────────────────────────────
        `when`("login_attempt 테이블의 컬럼 명세를 확인하면") {
            then("user_account_id 는 NULL 허용 BIGINT 이다 (계정 미존재 시도 허용)") {
                isNullable("login_attempt", "user_account_id") shouldBe true
                dataType("login_attempt", "user_account_id") shouldBe "bigint"
            }

            then("email 은 NOT NULL VARCHAR 이다") {
                isNullable("login_attempt", "email") shouldBe false
                dataType("login_attempt", "email") shouldBe "varchar"
            }

            then("attempted_at 은 NOT NULL TIMESTAMP 이다") {
                isNullable("login_attempt", "attempted_at") shouldBe false
                dataType("login_attempt", "attempted_at") shouldBe "timestamp"
            }

            then("success 는 NOT NULL TINYINT 이다") {
                isNullable("login_attempt", "success") shouldBe false
                dataType("login_attempt", "success") shouldBe "tinyint"
            }

            then("failure_reason 은 NULL 허용 VARCHAR 이다") {
                isNullable("login_attempt", "failure_reason") shouldBe true
                dataType("login_attempt", "failure_reason") shouldBe "varchar"
            }

            then("ip_address 는 NULL 허용 VARCHAR(45) 이다") {
                isNullable("login_attempt", "ip_address") shouldBe true
                columnCharLength("login_attempt", "ip_address") shouldBe 45
            }

            then("user_agent 는 NULL 허용 VARCHAR 이다") {
                isNullable("login_attempt", "user_agent") shouldBe true
                dataType("login_attempt", "user_agent") shouldBe "varchar"
            }

            then("created_at 은 NOT NULL TIMESTAMP 이다") {
                isNullable("login_attempt", "created_at") shouldBe false
                dataType("login_attempt", "created_at") shouldBe "timestamp"
            }

            then("updated_at 컬럼은 존재하지 않는다 (append-only)") {
                val count = connection.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = 'auth_db' AND TABLE_NAME = 'login_attempt' AND COLUMN_NAME = 'updated_at'",
                ).use { ps ->
                    ps.executeQuery().use { rs ->
                        rs.next()
                        rs.getInt(1)
                    }
                }
                count shouldBe 0
            }
        }

        `when`("login_attempt 테이블의 인덱스를 확인하면") {
            then("idx_login_attempt_account_at 인덱스가 존재한다") {
                hasIndex("login_attempt", "idx_login_attempt_account_at") shouldBe true
            }

            then("idx_login_attempt_email_at 인덱스가 존재한다") {
                hasIndex("login_attempt", "idx_login_attempt_email_at") shouldBe true
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // 7. two_factor_backup_code 테이블 검증
        // ─────────────────────────────────────────────────────────────────
        `when`("two_factor_backup_code 테이블의 컬럼 명세를 확인하면") {
            then("user_account_id 는 NOT NULL BIGINT 이다") {
                isNullable("two_factor_backup_code", "user_account_id") shouldBe false
                dataType("two_factor_backup_code", "user_account_id") shouldBe "bigint"
            }

            then("code_hash 는 NOT NULL CHAR(60) 이다 (bcrypt)") {
                isNullable("two_factor_backup_code", "code_hash") shouldBe false
                dataType("two_factor_backup_code", "code_hash") shouldBe "char"
                columnCharLength("two_factor_backup_code", "code_hash") shouldBe 60
            }

            then("used_at 은 NULL 허용 TIMESTAMP 이다") {
                isNullable("two_factor_backup_code", "used_at") shouldBe true
                dataType("two_factor_backup_code", "used_at") shouldBe "timestamp"
            }
        }

        `when`("two_factor_backup_code 테이블의 인덱스를 확인하면") {
            then("idx_two_factor_backup_code_account 인덱스가 존재한다") {
                hasIndex("two_factor_backup_code", "idx_two_factor_backup_code_account") shouldBe true
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // 8. api_token 테이블 검증
        // ─────────────────────────────────────────────────────────────────
        `when`("api_token 테이블의 컬럼 명세를 확인하면") {
            then("token_hash 는 NOT NULL CHAR(64) 이다") {
                isNullable("api_token", "token_hash") shouldBe false
                dataType("api_token", "token_hash") shouldBe "char"
                columnCharLength("api_token", "token_hash") shouldBe 64
            }

            then("scopes 는 NULL 허용 TEXT 이다 (JSON 미사용 — 콤마 구분 문자열)") {
                isNullable("api_token", "scopes") shouldBe true
                dataType("api_token", "scopes") shouldBe "text"
            }

            then("expires_at 은 NULL 허용 TIMESTAMP 이다 (무기한 허용)") {
                isNullable("api_token", "expires_at") shouldBe true
                dataType("api_token", "expires_at") shouldBe "timestamp"
            }

            then("last_used_at 은 NULL 허용 TIMESTAMP 이다") {
                isNullable("api_token", "last_used_at") shouldBe true
                dataType("api_token", "last_used_at") shouldBe "timestamp"
            }

            then("revoked_at 은 NULL 허용 TIMESTAMP 이다") {
                isNullable("api_token", "revoked_at") shouldBe true
                dataType("api_token", "revoked_at") shouldBe "timestamp"
            }

            then("name 은 NOT NULL VARCHAR(100) 이다") {
                isNullable("api_token", "name") shouldBe false
                dataType("api_token", "name") shouldBe "varchar"
                columnCharLength("api_token", "name") shouldBe 100
            }
        }

        `when`("api_token 테이블의 인덱스를 확인하면") {
            then("uq_api_token_hash UNIQUE 인덱스가 존재한다") {
                hasIndex("api_token", "uq_api_token_hash") shouldBe true
                isUniqueIndex("api_token", "uq_api_token_hash") shouldBe true
            }

            then("idx_api_token_account 인덱스가 존재한다") {
                hasIndex("api_token", "idx_api_token_account") shouldBe true
            }

            then("idx_api_token_deleted_at 인덱스가 존재한다") {
                hasIndex("api_token", "idx_api_token_deleted_at") shouldBe true
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // 9. 7개 테이블 COMMENT 존재 확인
        // ─────────────────────────────────────────────────────────────────
        `when`("7개 테이블의 TABLE COMMENT 를 확인하면") {
            val targetTables = listOf(
                "user_account",
                "role",
                "user_account_role",
                "refresh_token",
                "login_attempt",
                "two_factor_backup_code",
                "api_token",
            )

            for (table in targetTables) {
                then("$table 테이블 COMMENT 가 비어 있지 않다") {
                    tableComment(table).isNotBlank() shouldBe true
                }
            }
        }

        `when`("주요 컬럼의 COMMENT 를 확인하면") {
            then("user_account.status COMMENT 가 비어 있지 않다") {
                columnComment("user_account", "status").isNotBlank() shouldBe true
            }

            then("user_account.two_factor_secret COMMENT 가 비어 있지 않다") {
                columnComment("user_account", "two_factor_secret").isNotBlank() shouldBe true
            }

            then("api_token.scopes COMMENT 가 비어 있지 않다") {
                columnComment("api_token", "scopes").isNotBlank() shouldBe true
            }

            then("login_attempt.user_account_id COMMENT 가 비어 있지 않다") {
                columnComment("login_attempt", "user_account_id").isNotBlank() shouldBe true
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // 10. Flyway 체크섬 — V1 success=1
        // ─────────────────────────────────────────────────────────────────
        `when`("flyway_schema_history 를 확인하면") {
            val versions = mutableListOf<String>()
            val statuses = mutableListOf<String>()
            connection.createStatement().use { stmt ->
                stmt.executeQuery(
                    "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank",
                ).use { rs ->
                    while (rs.next()) {
                        versions.add(rs.getString(1))
                        statuses.add(rs.getString(2))
                    }
                }
            }

            then("V1 이 적용되어 있다") {
                versions.contains("1") shouldBe true
            }

            then("모든 마이그레이션이 success=1 이다") {
                statuses.all { it == "1" } shouldBe true
            }
        }

        connection.close()
    }
})
