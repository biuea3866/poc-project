package com.biuea.springdata.controller

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 파티셔닝 재검증 전용 엔드포인트.
 *
 * JPA 가 아닌 JdbcTemplate 을 쓰는 이유: 실험군과 대조군이 **문자 그대로 같은 SQL** 을 서로 다른
 * 테이블에만 던지도록 통제해야 한다. JPA 를 거치면 Page 의 count 쿼리·엔티티 매핑 비용이 붙어
 * 무엇이 차이를 만들었는지 분리할 수 없다.
 *
 * 실험 구성은 scripts/rebench/PLAN.md 참조.
 */
@RestController
@RequestMapping("/api/rebench")
class RebenchController(
    private val jdbcTemplate: JdbcTemplate
) {

    /**
     * 실험 A — 파티션 키를 포함한 범위 조회.
     * 세 테이블은 행 값·인덱스가 동일하고 PK 폭과 파티셔닝 여부만 다르다.
     */
    @GetMapping("/a/{table}")
    fun rangeQuery(
        @PathVariable table: String,
        @RequestParam startDate: String,
        @RequestParam endDate: String,
        @RequestParam(defaultValue = "date") orderBy: String,
        @RequestParam(defaultValue = "100") size: Int
    ): Map<String, Any> {
        // orderBy=date  : 세 테이블 모두 idx_created_date 레인지 스캔을 타 접근 방식이 같아진다.
        //                 파티셔닝 단독 효과를 보는 기본값이다.
        // orderBy=id    : 옵티마이저가 PK 순 스캔을 고른다. 비파티션 테이블은 2024 행에 닿기까지
        //                 앞선 연도를 훑지만, 파티션 테이블은 p2024 안에서 시작한다.
        val order = when (orderBy) {
            "date" -> "created_date, id"
            "id" -> "id"
            else -> throw IllegalArgumentException("unknown orderBy: $orderBy")
        }
        val rows = jdbcTemplate.queryForList(
            "SELECT id, name, created_date FROM ${productTable(table)} " +
                "WHERE created_date BETWEEN ? AND ? ORDER BY $order LIMIT ?",
            startDate, endDate, size
        )
        return mapOf("count" to rows.size, "rows" to rows)
    }

    /**
     * 실험 A'' — 같은 범위에 대한 집계.
     * LIMIT 100 조회는 인덱스 B-트리 하강만으로 시작 위치를 찾으므로 프루닝이 줄일 일이 거의 없다.
     * 반면 한 해 전체 행을 읽어야 하는 집계는 파티션 데이터 파일 하나로 스캔이 국한되는 이득이 드러난다.
     */
    @GetMapping("/agg/{table}")
    fun aggregate(
        @PathVariable table: String,
        @RequestParam startDate: String,
        @RequestParam endDate: String
    ): Map<String, Any?> {
        return jdbcTemplate.queryForMap(
            "SELECT COUNT(*) AS cnt, SUM(price) AS total, AVG(stock_quantity) AS avg_stock " +
                "FROM ${productTable(table)} WHERE created_date BETWEEN ? AND ?",
            startDate, endDate
        )
    }

    /**
     * 실험 B — 파티션 키를 포함하지 않은 PK 점 조회.
     * 파티션 테이블은 PK 가 (id, created_date) 라 id 만으로는 파티션을 좁히지 못하고
     * 모든 파티션의 PK 를 각각 뒤진다. 글이 예로 든 `WHERE id = 12345` 케이스다.
     */
    @GetMapping("/b/{table}")
    fun pointLookup(
        @PathVariable table: String,
        @RequestParam id: Long
    ): Map<String, Any> {
        val rows = jdbcTemplate.queryForList(
            "SELECT id, name, created_date FROM ${productTable(table)} WHERE id = ?",
            id
        )
        return mapOf("count" to rows.size, "rows" to rows)
    }

    /**
     * 실험 C — JOIN 에서 댓글 테이블의 프루닝 유무.
     * withKey=true 면 댓글에도 같은 날짜 범위를 건다. 댓글은 상품과 같은 해로 시드했으므로
     * 이 조건은 결과 집합을 바꾸지 않고 프루닝만 켠다.
     */
    @GetMapping("/c")
    fun joinQuery(
        @RequestParam startDate: String,
        @RequestParam endDate: String,
        @RequestParam idFrom: Long,
        @RequestParam idTo: Long,
        @RequestParam withKey: Boolean,
        @RequestParam(defaultValue = "100") size: Int
    ): Map<String, Any> {
        // 상품 쪽을 좁은 id 구간으로 제한한다. 그러지 않으면 p2024 전체 스캔이 비용을 지배해
        // 정작 재려는 댓글 테이블의 프루닝 차이가 묻힌다.
        val commentPredicate = if (withKey) "AND c.created_date BETWEEN ? AND ? " else ""
        val sql = "SELECT p.id AS product_id, p.name, c.id AS comment_id, c.rating " +
            "FROM product_partitioned p " +
            "JOIN comment c ON c.product_id = p.id " +
            "WHERE p.created_date BETWEEN ? AND ? AND p.id BETWEEN ? AND ? " +
            commentPredicate +
            "ORDER BY p.id, c.id LIMIT ?"

        val args: Array<Any> = if (withKey) {
            arrayOf(startDate, endDate, idFrom, idTo, startDate, endDate, size)
        } else {
            arrayOf(startDate, endDate, idFrom, idTo, size)
        }

        val rows = jdbcTemplate.queryForList(sql, *args)
        return mapOf("count" to rows.size, "rows" to rows)
    }

    /** 경로 변수를 테이블명으로 바꾼다. 허용 목록 밖의 값은 거부한다. */
    private fun productTable(table: String): String = when (table) {
        "plain" -> "product"
        "composite" -> "product_composite_pk"
        "partitioned" -> "product_partitioned"
        // 날짜 인덱스 대조 실험용 — 같은 구조에서 idx_created_date 만 뺀 짝
        "plain-noidx" -> "product_noidx"
        "partitioned-noidx" -> "product_partitioned_noidx"
        else -> throw IllegalArgumentException("unknown table: $table")
    }
}
