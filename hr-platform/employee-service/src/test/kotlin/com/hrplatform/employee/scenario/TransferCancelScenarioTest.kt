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
import java.util.UUID

/**
 * 발령 취소 보상 시나리오 E2E
 *
 * 1. POST /employees → 입사
 * 2. POST /employees/{id}/employment-events (DEPT_CHANGE: A→B)
 * 3. DELETE /employees/{id}/employment-events/{lastEventId} (취소)
 * 검증:
 * - EmploymentHistory에 cancelled_at 채워짐
 * - Kafka에 employee.transferred.cancelled 이벤트 발행
 * - action.type = "TRANSFER_CANCELLED", action.details.cancelledHistoryId 정확
 * - Employment.departmentId 원복 (B→A)
 */
class TransferCancelScenarioTest(
    @Autowired environment: Environment,
) : BaseE2eTest(environment) {

    private val employeeTopic = "event.hr.employee.v1"

    init {
        given("시나리오: 발령 취소 보상") {
            `when`("입사 → 부서 이동 → 발령 취소를 순서대로 실행하면") {
                // 실행마다 고유한 식별자 — DB uniqueness 충돌 방지
                val runId = UUID.randomUUID().toString().take(8)
                val consumer = buildKafkaConsumer("transfer-cancel-$runId")
                awaitPartitionAssignment(consumer, employeeTopic)

                val companyId = 5001
                val departmentIdA = 100L
                val departmentIdB = 200L

                // Step 1: 입사 등록 (부서A 소속)
                val hireRequest = mapOf(
                    "personalEmail" to "transfer.$runId@example.com",
                    "name" to "발령취소시나리오",
                    "companyId" to companyId,
                    "employeeNumber" to "TC-$runId",
                    "employmentType" to "REGULAR",
                    "startDate" to "2026-01-01",
                    "country" to "KR",
                    "currency" to "KRW",
                    "timezone" to "Asia/Seoul",
                    "departmentId" to departmentIdA,
                )
                // 입사 시 actor = 0 (system) — authorizatin resolver가 null 허용
                val hireHeaders = HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                }
                val hireResponse = restTemplate.postForEntity(
                    baseUrl("/employees"),
                    HttpEntity(objectMapper.writeValueAsString(hireRequest), hireHeaders),
                    Map::class.java,
                )

                then("입사 등록이 201 Created로 성공한다") {
                    hireResponse.statusCode shouldBe HttpStatus.CREATED
                }

                val employmentId = (hireResponse.body!!["employmentId"] as Number).toLong()

                // Step 2: 부서 이동 (A→B)
                // viewer = 본인 (ACTIVE 상태)
                val recordEventRequest = mapOf(
                    "eventType" to "DEPT_CHANGE",
                    "newDepartmentId" to departmentIdB,
                    "effectiveDate" to "2026-03-01",
                )
                val recordHeaders = HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    set("X-Employment-Id", employmentId.toString())
                }
                val recordResponse = restTemplate.postForEntity(
                    baseUrl("/employees/$employmentId/employment-events"),
                    HttpEntity(objectMapper.writeValueAsString(recordEventRequest), recordHeaders),
                    Map::class.java,
                )

                then("부서 이동이 성공하고 departmentId가 B로 변경된다") {
                    recordResponse.statusCode shouldBe HttpStatus.OK
                    recordResponse.body shouldNotBe null
                    (recordResponse.body!!["departmentId"] as Number).toLong() shouldBe departmentIdB
                }

                // Step 3: 최신 이력 조회 — DEPT_CHANGE 이벤트 historyId 확인
                // viewer = 본인 (ACTIVE)
                val historyHeaders = HttpHeaders().apply {
                    set("X-Employment-Id", employmentId.toString())
                }
                val historyResponse = restTemplate.exchange(
                    baseUrl("/employees/$employmentId/employment-events"),
                    HttpMethod.GET,
                    HttpEntity<Unit>(historyHeaders),
                    List::class.java,
                )

                val transferHistoryId = run {
                    val histories = historyResponse.body as? List<*>
                    histories shouldNotBe null
                    val transferHistory = histories!!.filterIsInstance<Map<*, *>>()
                        .firstOrNull { it["eventType"] == "DEPT_CHANGE" }
                    transferHistory shouldNotBe null
                    (transferHistory!!["historyId"] as Number).toLong()
                }

                // Step 4: 발령 취소
                val cancelRequest = mapOf("cancellationReason" to "오기입 취소 시나리오")
                val cancelHeaders = HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    set("X-Employment-Id", employmentId.toString())
                }
                val cancelResponse = restTemplate.exchange(
                    baseUrl("/employees/$employmentId/employment-events/$transferHistoryId"),
                    HttpMethod.DELETE,
                    HttpEntity(objectMapper.writeValueAsString(cancelRequest), cancelHeaders),
                    Map::class.java,
                )

                then("발령 취소가 성공한다") {
                    cancelResponse.statusCode shouldBe HttpStatus.OK
                }

                // Step 5: 이력 재조회 — cancelledAt이 채워져 있어야 한다
                val historyAfterCancelResponse = restTemplate.exchange(
                    baseUrl("/employees/$employmentId/employment-events"),
                    HttpMethod.GET,
                    HttpEntity<Unit>(historyHeaders),
                    List::class.java,
                )

                then("EmploymentHistory의 DEPT_CHANGE 이벤트에 cancelledAt이 채워진다") {
                    val histories = historyAfterCancelResponse.body as? List<*>
                    histories shouldNotBe null
                    val cancelledHistory = histories!!.filterIsInstance<Map<*, *>>()
                        .firstOrNull { (it["historyId"] as? Number)?.toLong() == transferHistoryId }
                    cancelledHistory shouldNotBe null
                    cancelledHistory!!["cancelledAt"] shouldNotBe null
                }

                // Step 6: Kafka 이벤트 검증 — HIRE + TRANSFER + TRANSFER_CANCELLED = 3건
                then("Kafka에 employee.transferred.cancelled 이벤트가 발행된다") {
                    // seekToEnd로 현재 위치에서 시작하므로 이 테스트 실행의 3건만 수신된다
                    val rawMessages = pollMessages(consumer, expectedCount = 3)
                    consumer.close()

                    val parsedMessages = rawMessages.map { objectMapper.readValue(it, Map::class.java) }
                    val cancelledEvent = parsedMessages.firstOrNull { it["eventType"] == "EmployeeTransferredCancelled" }
                    cancelledEvent shouldNotBe null
                    cancelledEvent!!["aggregateId"] shouldBe employmentId.toInt()

                    val action = cancelledEvent["action"] as? Map<*, *>
                    action shouldNotBe null
                    action!!["type"] shouldBe "TRANSFER_CANCELLED"

                    val details = action["details"] as? Map<*, *>
                    details shouldNotBe null
                    (details!!["cancelledHistoryId"] as? Number)?.toLong() shouldBe transferHistoryId
                }
            }
        }
    }
}
