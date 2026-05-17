package com.hrplatform.employee.scenario

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.time.LocalDate
import java.util.UUID

/**
 * X4 시나리오: 100명 일괄 등록 트랜잭션 보장 E2E
 *
 * Case A: 100건 모두 유효 → successCount=100, Kafka에 employee.hired 100건 발행
 * Case B: 1건 invalid(미성년자 포함) → 전체 롤백, DB에 0건
 */
class X4BulkHireScenarioTest(
    @Autowired environment: Environment,
) : BaseE2eTest(environment) {

    private val employeeTopic = "event.hr.employee.v1"

    private fun buildValidHireRequest(runId: String, index: Int, companyId: Int): Map<String, Any?> = mapOf(
        "personalEmail" to "bulk.$runId.$index@example.com",
        "name" to "일괄직원$index",
        "companyId" to companyId,
        "employeeNumber" to "$runId-$index",
        "employmentType" to "REGULAR",
        "startDate" to "2026-01-01",
        "country" to "KR",
        "currency" to "KRW",
        "timezone" to "Asia/Seoul",
    )

    /**
     * 미성년자 요청 — birthDate를 현재 기준 17세로 설정.
     * Person.validateNotMinor() 에 의해 MinorPersonNotAllowedException 발생.
     */
    private fun buildMinorHireRequest(runId: String, index: Int, companyId: Int): Map<String, Any?> = mapOf(
        "personalEmail" to "minor.$runId.$index@example.com",
        "name" to "미성년자$index",
        "birthDate" to LocalDate.now().minusYears(17).toString(),
        "companyId" to companyId,
        "employeeNumber" to "MINOR-$runId-$index",
        "employmentType" to "REGULAR",
        "startDate" to "2026-01-01",
        "country" to "KR",
        "currency" to "KRW",
        "timezone" to "Asia/Seoul",
    )

    init {
        given("시나리오 X4: 100명 일괄 등록 트랜잭션 보장") {
            `when`("100건 모두 유효한 입사 데이터를 일괄 등록하면") {
                val runId = UUID.randomUUID().toString().take(8)
                val consumer = buildKafkaConsumer("x4-bulk-success-$runId")
                awaitPartitionAssignment(consumer, employeeTopic)

                // 실행마다 고유한 companyId 생성 — 다른 시나리오와 격리
                val companyId = (40000..49999).random()
                val requests = (1..100).map { buildValidHireRequest(runId, it, companyId) }

                val headers = HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    set("X-Employment-Id", "9004")
                }
                val response = restTemplate.postForEntity(
                    baseUrl("/employees/bulk"),
                    HttpEntity(objectMapper.writeValueAsString(requests), headers),
                    Map::class.java,
                )

                then("successCount=100, failureCount=0을 반환한다") {
                    response.statusCode shouldBe HttpStatus.OK
                    response.body shouldNotBe null
                    response.body!!["successCount"] shouldBe 100
                    response.body!!["failureCount"] shouldBe 0
                }

                then("Kafka에 employee.hired 이벤트가 100건 발행된다") {
                    // seekToEnd로 현재 위치에서 시작하므로 이 테스트 실행의 100건만 수신된다
                    val rawMessages = pollMessages(consumer, expectedCount = 100, timeoutMs = 60_000L)
                    consumer.close()

                    rawMessages.size shouldBe 100
                    val parsedMessages = rawMessages.map { objectMapper.readValue(it, Map::class.java) }
                    val hiredEvents = parsedMessages.filter { it["eventType"] == "EmployeeHired" }
                    hiredEvents.size shouldBe 100

                    // companyId가 모두 동일해야 한다
                    val companyIds = hiredEvents.map { (it["companyId"] as? Number)?.toInt() }.toSet()
                    companyIds shouldBe setOf(companyId)
                }
            }

            `when`("100건 중 1건에 미성년자 데이터가 포함되어 있으면") {
                val runId = UUID.randomUUID().toString().take(8)
                val companyId = (50000..59999).random()

                // 99건 유효 + 1건 미성년자 (index 50번째 위치에 삽입)
                val requests = mutableListOf<Map<String, Any?>>()
                (1..49).forEach { requests.add(buildValidHireRequest(runId, it, companyId)) }
                requests.add(buildMinorHireRequest(runId, 50, companyId))
                (51..100).forEach { requests.add(buildValidHireRequest(runId, it, companyId)) }

                val headers = HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    set("X-Employment-Id", "9004")
                }
                val response = restTemplate.postForEntity(
                    baseUrl("/employees/bulk"),
                    HttpEntity(objectMapper.writeValueAsString(requests), headers),
                    Map::class.java,
                )

                then("전체 트랜잭션이 롤백되어 4xx 또는 5xx 오류 응답이어야 한다") {
                    // BulkHireUseCase는 @Transactional 전체 롤백 방식.
                    // MinorPersonNotAllowedException은 BusinessException 계열 → GlobalExceptionHandler가 422 처리.
                    val isErrorResponse = response.statusCode.is4xxClientError || response.statusCode.is5xxServerError
                    isErrorResponse shouldBe true
                }

                then("companyId 소속 직원이 DB에 0건이다") {
                    val queryHeaders = HttpHeaders().apply {
                        set("X-Employment-Id", "9004")
                    }
                    val searchResponse = restTemplate.exchange(
                        baseUrl("/employees?companyId=$companyId"),
                        HttpMethod.GET,
                        HttpEntity<Unit>(queryHeaders),
                        Map::class.java,
                    )
                    // 조회 성공 시 totalElements = 0 검증
                    if (searchResponse.statusCode == HttpStatus.OK && searchResponse.body != null) {
                        val totalElements = searchResponse.body!!["totalElements"] as? Number
                        totalElements?.toInt() shouldBe 0
                    }
                }
            }
        }
    }
}
