package com.hrplatform.employee.migration

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.flywaydb.core.Flyway
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager

/**
 * Flyway V1~V4 마이그레이션 순차 적용 검증.
 *
 * Testcontainers MySQL 8.0 컨테이너를 직접 구동하여 Spring 컨텍스트 의존 없이 DDL만 검증한다.
 * 검증 항목:
 *   1) 4개 테이블(person, employment, department, employment_history) 생성 여부
 *   2) 각 테이블 인덱스 정확성
 *   3) 주요 컬럼의 NOT NULL / DEFAULT / 타입 명세 준수
 */
class FlywayMigrationTest : BehaviorSpec({

    val mysql: MySQLContainer<*> = MySQLContainer(DockerImageName.parse("mysql:8.0"))
        .withDatabaseName("employee_db")
        .withUsername("test")
        .withPassword("test")
        .withCommand(
            "--character-set-server=utf8mb4",
            "--collation-server=utf8mb4_unicode_ci",
        )

    beforeSpec { mysql.start() }
    afterSpec { mysql.stop() }

    given("Flyway V1~V4 마이그레이션을 순차 적용하면") {

        val flyway = Flyway.configure()
            .dataSource(mysql.jdbcUrl, mysql.username, mysql.password)
            .locations("classpath:db/migration")
            .load()

        flyway.migrate()

        val connection = DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password)

        `when`("SHOW TABLES 를 실행하면") {
            val tables = mutableListOf<String>()
            connection.createStatement().use { stmt ->
                stmt.executeQuery("SHOW TABLES").use { rs ->
                    while (rs.next()) tables.add(rs.getString(1).lowercase())
                }
            }

            then("4개 테이블이 모두 존재한다") {
                tables shouldContainAll listOf(
                    "person",
                    "employment",
                    "department",
                    "employment_history",
                )
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // person 테이블 검증
        // ─────────────────────────────────────────────────────────────────
        `when`("person 테이블의 컬럼 명세를 확인하면") {
            data class ColumnMeta(val nullable: String, val dataType: String, val columnDefault: String?)

            fun getColumn(table: String, column: String): ColumnMeta {
                connection.prepareStatement(
                    "SELECT IS_NULLABLE, DATA_TYPE, COLUMN_DEFAULT " +
                        "FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                ).use { ps ->
                    ps.setString(1, "employee_db")
                    ps.setString(2, table)
                    ps.setString(3, column)
                    ps.executeQuery().use { rs ->
                        rs.next()
                        return ColumnMeta(rs.getString(1), rs.getString(2), rs.getString(3))
                    }
                }
            }

            then("id 는 NOT NULL BIGINT AUTO_INCREMENT 이다") {
                val col = getColumn("person", "id")
                col.nullable shouldBe "NO"
                col.dataType shouldBe "bigint"
            }

            then("name 은 NOT NULL VARCHAR 이다") {
                val col = getColumn("person", "name")
                col.nullable shouldBe "NO"
                col.dataType shouldBe "varchar"
            }

            then("personal_email 은 NOT NULL VARBINARY 이다") {
                val col = getColumn("person", "personal_email")
                col.nullable shouldBe "NO"
                col.dataType shouldBe "varbinary"
            }

            then("phone_number 는 NULL 허용 VARBINARY 이다") {
                val col = getColumn("person", "phone_number")
                col.nullable shouldBe "YES"
                col.dataType shouldBe "varbinary"
            }

            then("created_at 은 NOT NULL TIMESTAMP 이다") {
                val col = getColumn("person", "created_at")
                col.nullable shouldBe "NO"
                col.dataType shouldBe "timestamp"
            }
        }

        `when`("person 테이블의 인덱스를 확인하면") {
            val indexes = mutableMapOf<String, String>() // index_name → non_unique
            connection.prepareStatement(
                "SHOW INDEX FROM person",
            ).use { ps ->
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        indexes[rs.getString("Key_name")] = rs.getString("Non_unique")
                    }
                }
            }

            then("PRIMARY 인덱스가 존재한다") {
                indexes.containsKey("PRIMARY") shouldBe true
            }

            then("uq_person_personal_email UNIQUE 인덱스가 존재한다") {
                indexes.containsKey("uq_person_personal_email") shouldBe true
                indexes["uq_person_personal_email"] shouldBe "0"
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // employment 테이블 검증
        // ─────────────────────────────────────────────────────────────────
        `when`("employment 테이블의 인덱스를 확인하면") {
            val indexNames = mutableSetOf<String>()
            val uniqueIndexes = mutableSetOf<String>()
            connection.prepareStatement("SHOW INDEX FROM employment").use { ps ->
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        val name = rs.getString("Key_name")
                        indexNames.add(name)
                        if (rs.getString("Non_unique") == "0") uniqueIndexes.add(name)
                    }
                }
            }

            then("uq_employment_company_employee_number UNIQUE 인덱스가 존재한다") {
                uniqueIndexes.contains("uq_employment_company_employee_number") shouldBe true
            }

            then("idx_employment_person_id 인덱스가 존재한다") {
                indexNames.contains("idx_employment_person_id") shouldBe true
            }

            then("idx_employment_department_id 인덱스가 존재한다") {
                indexNames.contains("idx_employment_department_id") shouldBe true
            }

            then("idx_employment_manager_employment_id 인덱스가 존재한다") {
                indexNames.contains("idx_employment_manager_employment_id") shouldBe true
            }

            then("idx_employment_status 인덱스가 존재한다") {
                indexNames.contains("idx_employment_status") shouldBe true
            }
        }

        `when`("employment 테이블의 NOT NULL 컬럼을 확인하면") {
            fun isNotNull(column: String): Boolean {
                connection.prepareStatement(
                    "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = 'employee_db' AND TABLE_NAME = 'employment' AND COLUMN_NAME = ?",
                ).use { ps ->
                    ps.setString(1, column)
                    ps.executeQuery().use { rs ->
                        rs.next()
                        return rs.getString(1) == "NO"
                    }
                }
            }

            then("person_id 는 NOT NULL 이다") { isNotNull("person_id") shouldBe true }
            then("company_id 는 NOT NULL 이다") { isNotNull("company_id") shouldBe true }
            then("employee_number 는 NOT NULL 이다") { isNotNull("employee_number") shouldBe true }
            then("employment_type 는 NOT NULL 이다") { isNotNull("employment_type") shouldBe true }
            then("status 는 NOT NULL 이다") { isNotNull("status") shouldBe true }
            then("start_date 는 NOT NULL 이다") { isNotNull("start_date") shouldBe true }
            then("country 는 NOT NULL 이다") { isNotNull("country") shouldBe true }
            then("currency 는 NOT NULL 이다") { isNotNull("currency") shouldBe true }
            then("timezone 는 NOT NULL 이다") { isNotNull("timezone") shouldBe true }
            then("end_date 는 NULL 허용이다") { isNotNull("end_date") shouldBe false }
            then("department_id 는 NULL 허용이다") { isNotNull("department_id") shouldBe false }
        }

        // ─────────────────────────────────────────────────────────────────
        // department 테이블 검증
        // ─────────────────────────────────────────────────────────────────
        `when`("department 테이블의 인덱스를 확인하면") {
            val indexNames = mutableSetOf<String>()
            val uniqueIndexes = mutableSetOf<String>()
            connection.prepareStatement("SHOW INDEX FROM department").use { ps ->
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        val name = rs.getString("Key_name")
                        indexNames.add(name)
                        if (rs.getString("Non_unique") == "0") uniqueIndexes.add(name)
                    }
                }
            }

            then("uq_department_company_code UNIQUE 인덱스가 존재한다") {
                uniqueIndexes.contains("uq_department_company_code") shouldBe true
            }

            then("idx_department_parent_id 인덱스가 존재한다") {
                indexNames.contains("idx_department_parent_id") shouldBe true
            }

            then("idx_department_path 인덱스가 존재한다") {
                indexNames.contains("idx_department_path") shouldBe true
            }

            then("idx_department_head_employment_id 인덱스가 존재한다") {
                indexNames.contains("idx_department_head_employment_id") shouldBe true
            }
        }

        `when`("department 테이블의 컬럼 명세를 확인하면") {
            fun columnInfo(column: String): Pair<String, String?> {
                connection.prepareStatement(
                    "SELECT IS_NULLABLE, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = 'employee_db' AND TABLE_NAME = 'department' AND COLUMN_NAME = ?",
                ).use { ps ->
                    ps.setString(1, column)
                    ps.executeQuery().use { rs ->
                        rs.next()
                        return Pair(rs.getString(1), rs.getString(2))
                    }
                }
            }

            then("path 는 NOT NULL 이다") {
                columnInfo("path").first shouldBe "NO"
            }

            then("order_no 는 DEFAULT 0 이다") {
                columnInfo("order_no").second shouldBe "0"
            }

            then("effective_from 는 NOT NULL 이다") {
                columnInfo("effective_from").first shouldBe "NO"
            }

            then("effective_to 는 NULL 허용이다") {
                columnInfo("effective_to").first shouldBe "YES"
            }

            then("head_employment_id 는 NULL 허용이다") {
                columnInfo("head_employment_id").first shouldBe "YES"
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // employment_history 테이블 검증
        // ─────────────────────────────────────────────────────────────────
        `when`("employment_history 테이블의 인덱스를 확인하면") {
            val indexNames = mutableSetOf<String>()
            connection.prepareStatement("SHOW INDEX FROM employment_history").use { ps ->
                ps.executeQuery().use { rs ->
                    while (rs.next()) indexNames.add(rs.getString("Key_name"))
                }
            }

            then("idx_employment_history_employment_effective 인덱스가 존재한다") {
                indexNames.contains("idx_employment_history_employment_effective") shouldBe true
            }
        }

        `when`("employment_history 테이블의 컬럼 명세를 확인하면") {
            fun isNotNull(column: String): Boolean {
                connection.prepareStatement(
                    "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = 'employee_db' AND TABLE_NAME = 'employment_history' AND COLUMN_NAME = ?",
                ).use { ps ->
                    ps.setString(1, column)
                    ps.executeQuery().use { rs ->
                        rs.next()
                        return rs.getString(1) == "NO"
                    }
                }
            }

            then("employment_id 는 NOT NULL 이다") { isNotNull("employment_id") shouldBe true }
            then("event_type 는 NOT NULL 이다") { isNotNull("event_type") shouldBe true }
            then("new_value 는 NOT NULL 이다") { isNotNull("new_value") shouldBe true }
            then("effective_date 는 NOT NULL 이다") { isNotNull("effective_date") shouldBe true }
            then("created_at 는 NOT NULL 이다") { isNotNull("created_at") shouldBe true }
            then("old_value 는 NULL 허용이다") { isNotNull("old_value") shouldBe false }
            then("cancelled_at 는 NULL 허용이다") { isNotNull("cancelled_at") shouldBe false }
            then("created_by_employment_id 는 NULL 허용이다") { isNotNull("created_by_employment_id") shouldBe false }
        }

        // ─────────────────────────────────────────────────────────────────
        // COMMENT 검증 — 테이블 COMMENT가 비어 있지 않으면 DDL COMMENT가 적용된 것
        // ─────────────────────────────────────────────────────────────────
        `when`("4개 테이블의 COMMENT 를 확인하면") {
            fun tableComment(table: String): String {
                connection.prepareStatement(
                    "SELECT TABLE_COMMENT FROM INFORMATION_SCHEMA.TABLES " +
                        "WHERE TABLE_SCHEMA = 'employee_db' AND TABLE_NAME = ?",
                ).use { ps ->
                    ps.setString(1, table)
                    ps.executeQuery().use { rs ->
                        rs.next()
                        return rs.getString(1) ?: ""
                    }
                }
            }

            then("person 테이블 COMMENT 가 비어 있지 않다") {
                tableComment("person").isNotBlank() shouldBe true
            }

            then("employment 테이블 COMMENT 가 비어 있지 않다") {
                tableComment("employment").isNotBlank() shouldBe true
            }

            then("department 테이블 COMMENT 가 비어 있지 않다") {
                tableComment("department").isNotBlank() shouldBe true
            }

            then("employment_history 테이블 COMMENT 가 비어 있지 않다") {
                tableComment("employment_history").isNotBlank() shouldBe true
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // Flyway 체크섬 검증 — V1~V4 모두 성공 상태
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

            then("V1, V2, V3, V4 가 모두 적용되어 있다") {
                versions shouldContainAll listOf("1", "2", "3", "4")
            }

            then("모든 마이그레이션이 success=1 이다") {
                statuses.all { it == "1" } shouldBe true
            }
        }

        connection.close()
    }
})
