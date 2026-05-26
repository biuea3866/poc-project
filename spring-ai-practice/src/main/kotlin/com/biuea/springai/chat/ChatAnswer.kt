package com.biuea.springai.chat

/**
 * `/chat` LLM 응답의 구조화 출력 타겟 — 작은 모델 친화적으로 필드를 축소.
 *
 * `BeanOutputConverter` 가 이 클래스의 JSON Schema 를 LLM 프롬프트에 자동 추가하고,
 * 모델 응답을 ObjectMapper 로 역직렬화한다.
 *
 * 필드:
 * - `answer` — 사용자에게 보여줄 자연어 응답 (한국어 해요체)
 * - `intent` — 분류된 의도 한 단어 (예: `SEARCH`, `ORDER`, `POLICY`, `OTHER`)
 *
 * 처음 PoC 에서는 mentionedProductIds / mentionedOrderIds / suggestedFollowUps 도 포함했으나
 * 작은 모델(llama3.2:3b 등) 이 List<String> 필드를 채우는 데 비용이 커서 응답 시간이 240s+ 가 됐다.
 * 두 필드만 남겨 LLM 출력 토큰 수를 최소화한다.
 */
data class ChatAnswer(
    val answer: String = "",
    val intent: String? = null,
)
