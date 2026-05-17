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
 * X1 시나리오: 퇴사 처리 E2E
 *
 * 1. POST /employees → HR Manager(actor) 입사 등록
 * 2. POST /employees → 대상 직원 입사 등록
 * 3. POST /employees/{id}/resign → 퇴사 처리
 * 검증:
 * - Employment.status = RESIGNED
 * - GET /employees/{id}/employment-events → EmploymentHistory에 RESIGN 이벤트 1건 추가
 * - Kafka event.hr.employee.v1 토픽에 employee.resigned 이벤트 발행
 * - payload action.type = "RESIGN", state.status = "RESIGNED"
 */
class X1ResignScenarioTest(
    @Autowired environment: Environment,
) : BaseE2eTest(environment) {

    private val employeeTopic = "event.hr.employee.v1"

    private fun hireEmployee(runId: String, suffix: String, companyId: Int): Long {
        val request = mapOf(
            "personalEmail" to "$suffix.$runId@example.com",
            "name" to "직원$suffix",
            "companyId" to companyId,
            "employeeNumber" to "$suffix-$runId",
            "employmentType" to "REGULAR",
            "startDate" to "2026-01-01",
            "country" to "KR",
            "currency" to "KRW",
            "timezone" to "Asia/Seoul",
        )
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("X-Employment-Id", "0")
        }
        val response = restTemplate.postForEntity(
            baseUrl("/employees"),
            HttpEntity(objectMapper.writeValueAsString(request), headers),
            Map::class.java,
        )
        return (response.body!!["employmentId"] as Number).toLong()
    }

    init {
        given("시나리오 X1: 퇴사 처리 E2E") {
            `when`("입사 등록 후 퇴사 처리를 하면") {
                // 실행마다 고유한 식별자 생성 — DB uniqueness 충돌 방지
                val runId = UUID.randomUUID().toString().take(8)
                val companyId = 1001
                val consumer = buildKafkaConsumer("x1-resign-$runId")
                awaitPartitionAssignment(consumer, employeeTopic)

                // Step 1: HR actor도 입사 등록 (viewer로 사용)
                val actorEmploymentId = hireEmployee(runId, "actor", companyId)

                // Step 2: 퇴사 대상 직원 입사 등록
                val targetEmploymentId = hireEmployee(runId, "target", companyId)

                then("입사 등록이 모두 성공한다") {
                    actorEmploymentId shouldNotBe null
                    targetEmploymentId shouldNotBe null
                }

                // Step 3: 퇴사 처리
                val resignRequest = mapOf("reason" to "개인 사유 시나리오")
                val resignHeaders = HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    set("X-Employment-Id", actorEmploymentId.toString())
                }
                val resignResponse = restTemplate.postForEntity(
                    baseUrl("/employees/$targetEmploymentId/resign"),
                    HttpEntity(objectMapper.writeValueAsString(resignRequest), resignHeaders),
                    Map::class.java,
                )

                then("퇴사 처리가 200 OK로 성공하고 status = RESIGNED 이다") {
                    resignResponse.statusCode shouldBe HttpStatus.OK
                    resignResponse.body shouldNotBe null
                    resignResponse.body!!["status"] shouldBe "RESIGNED"
                }

                // Step 4: 발령 이력 조회 — RESIGN 이벤트가 포함되어야 한다
                // viewer: ACTIVE 상태의 actor를 사용
                val historyHeaders = HttpHeaders().apply {
                    set("X-Employment-Id", actorEmploymentId.toString())
                }
                val historyResponse = restTemplate.exchange(
                    baseUrl("/employees/$targetEmploymentId/employment-events"),
                    HttpMethod.GET,
                    HttpEntity<Unit>(historyHeaders),
                    List::class.java,
                )

                then("EmploymentHistory에 RESIGN 이벤트 1건이 추가되어 있다") {
                    historyResponse.statusCode shouldBe HttpStatus.OK
                    val histories = historyResponse.body as? List<*>
                    histories shouldNotBe null
                    val resignHistory = histories!!.filterIsInstance<Map<*, *>>()
                        .firstOrNull { it["eventType"] == "RESIGN" }
                    resignHistory shouldNotBe null
                    resignHistory!!["cancelledAt"] shouldBe null
                }

                // Step 5: Kafka 이벤트 검증 — actor HIRE + target HIRE + RESIGN = 3건
                then("Kafka에 employee.resigned 이벤트가 발행된다") {
                    // seekToEnd로 현재 위치에서 시작하므로 이 테스트 실행의 3건만 수신된다
                    val rawMessages = pollMessages(consumer, expectedCount = 3)
                    consumer.close()

                    val parsedMessages = rawMessages.map { objectMapper.readValue(it, Map::class.java) }
                    val resignedEvent = parsedMessages.firstOrNull { it["eventType"] == "EmployeeResigned" }
                    resignedEvent shouldNotBe null
                    resignedEvent!!["aggregateId"] shouldBe targetEmploymentId.toInt()

                    val action = resignedEvent["action"] as? Map<*, *>
                    action shouldNotBe null
                    action!!["type"] shouldBe "RESIGN"

                    val state = resignedEvent["state"] as? Map<*, *>
                    state shouldNotBe null
                    state!!["status"] shouldBe "RESIGNED"
                }
            }
        }
    }
}
