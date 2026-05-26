package com.biuea.springai.service

import org.springframework.ai.document.Document
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory

/**
 * 앱 시작 시 RAG 인덱스를 1회 채우는 ApplicationRunner.
 *
 * Spring AI 의 핵심 RAG 빌딩 블록 3종을 사용한다.
 *
 * 1. **`Document`** — RAG 문서의 표준 표현. `text` + `metadata(map)` + 자동 생성 `id`.
 *    LLM 컨텍스트로 주입되는 단위가 된다.
 *
 * 2. **`TokenTextSplitter`** — 긴 문서를 토큰 기준 청크로 자르는 transformer.
 *    LLM 컨텍스트 한도와 임베딩 정밀도의 균형을 맞추기 위해 권장 기본값(800/350/5/10000) 사용.
 *
 * 3. **`VectorStore.add(documents)`** — 청크별로 EmbeddingModel 을 호출해 벡터화 후 인덱스에 추가.
 *    추후 `similaritySearch(query)` 의 검색 대상이 된다.
 *
 * 의도적으로 동기 호출이고, 실패하면 앱이 부팅되지 않는다 (Ollama 미실행 시 인지 가능).
 */
@Component
class KnowledgeBaseLoader(
    private val vectorStore: VectorStore,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)
    private val sourcePaths = listOf(
        "rag/size-guide.md",
        "rag/faq.md",
        "rag/care-guide.md",
    )

    override fun run(args: ApplicationArguments?) {
        val rawDocuments = sourcePaths.map { path ->
            val content = ClassPathResource(path).inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            Document.builder()
                .text(content)
                .metadata(mapOf("source" to path))
                .build()
        }

        val splitter = TokenTextSplitter()
        val chunks = splitter.apply(rawDocuments)

        try {
            vectorStore.add(chunks)
            log.info("KnowledgeBase 적재 완료: ${sourcePaths.size}개 문서 → ${chunks.size}개 청크 인덱싱")
        } catch (e: Exception) {
            log.warn(
                "KnowledgeBase 적재 실패 — RAG 검색이 비활성됩니다. " +
                    "원인: ${e.message}. (Ollama 에 'ollama pull nomic-embed-text' 가 필요할 수 있습니다.)",
            )
        }
    }
}
