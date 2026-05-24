package com.biuea.springai

import com.biuea.springai.service.KnowledgeBaseLoader
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

/**
 * 컨텍스트 로드 스모크 테스트.
 * ChatModel · EmbeddingModel · KnowledgeBaseLoader 를 모킹해 Ollama 서버 없이도
 * 전체 빈 그래프(ChatClient · ChatMemory · VectorStore · QuestionAnswerAdvisor ·
 * CatalogTools · ExtractionTool · ToolGuard · MCP 서버 · 보안 필터 · ChatGateway)가 구성되는지 검증한다.
 */
@SpringBootTest
class SpringAiPracticeApplicationTests {

    @MockitoBean
    lateinit var chatModel: ChatModel

    @MockitoBean
    lateinit var embeddingModel: EmbeddingModel

    // RAG 인덱싱(ApplicationRunner) 은 실 Ollama 임베딩 호출을 일으키므로 테스트에서는 비활성.
    @MockitoBean
    lateinit var knowledgeBaseLoader: KnowledgeBaseLoader

    @Test
    fun contextLoads() {
    }
}
