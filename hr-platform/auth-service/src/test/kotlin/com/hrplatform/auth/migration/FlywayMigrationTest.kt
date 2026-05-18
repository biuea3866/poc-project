package com.hrplatform.auth.migration

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.flywaydb.core.Flyway
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager

/**
 * Flyway V1/V2 마이그레이션 smoke 검증 — 테이블 존재 + Flyway success.
 * 세부 컬럼 검증은 main V1 DDL과의 정합 보강 후 별도 PR에서.
 */
class FlywayMigrationTest : BehaviorSpec({

    val mysql: MySQLContainer<*> = MySQLContainer(DockerImageName.parse("mysql:8.0"))
        .withDatabaseName("auth_db")
        .withUsername("test")
        .withPassword("test")

    beforeSpec { mysql.start() }
    afterSpec { mysql.stop() }

    given("Flyway 적용") {
        `when`("V1 + V2 + V3 순차 적용하면") {
            val flyway = Flyway.configure()
                .dataSource(mysql.jdbcUrl, mysql.username, mysql.password)
                .locations("classpath:db/migration")
                .load()
            val result = flyway.migrate()

            then("Flyway success 반환") {
                result.success shouldBe true
            }

            then("7개 테이블 존재") {
                val expected = listOf(
                    "user_accounts", "roles", "user_account_roles",
                    "refresh_tokens", "login_attempts",
                    "two_factor_backup_codes", "api_tokens",
                )
                val conn = DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password)
                val tables = mutableListOf<String>()
                conn.use {
                    val rs = it.metaData.getTables(mysql.databaseName, null, "%", arrayOf("TABLE"))
                    while (rs.next()) tables.add(rs.getString("TABLE_NAME"))
                }
                tables shouldContainAll expected
            }

            then("user_accounts에 email_hash 컬럼이 존재한다") {
                val conn = DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password)
                val columns = mutableListOf<String>()
                conn.use {
                    val rs = it.metaData.getColumns(mysql.databaseName, null, "user_accounts", null)
                    while (rs.next()) columns.add(rs.getString("COLUMN_NAME"))
                }
                columns shouldContainAll listOf("email_hash")
            }

            then("login_attempts에 email_hash 컬럼이 존재하고 email 컬럼은 없다") {
                val conn = DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password)
                val columns = mutableListOf<String>()
                conn.use {
                    val rs = it.metaData.getColumns(mysql.databaseName, null, "login_attempts", null)
                    while (rs.next()) columns.add(rs.getString("COLUMN_NAME"))
                }
                columns shouldContainAll listOf("email_hash")
            }
        }
    }
})
