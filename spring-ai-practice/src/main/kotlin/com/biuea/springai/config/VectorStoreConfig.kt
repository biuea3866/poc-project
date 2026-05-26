package com.biuea.springai.config

import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.SimpleVectorStore
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring AI 의 RAG(Retrieval-Augmented Generation) 인덱스 컴포넌트 구성.
 *
 * - **`EmbeddingModel`** — 텍스트를 부동소수 벡터(예: 768/1024 차원)로 변환하는 모델 추상화.
 *   Ollama starter 가 `application.yml` 의 `spring.ai.ollama.embedding.options.model` 에 따라
 *   `OllamaEmbeddingModel` 빈을 자동 등록한다. 우리는 `nomic-embed-text` 를 사용.
 *
 * - **`VectorStore`** — 임베딩 벡터 + 원본 텍스트(`Document`)를 함께 보관하고,
 *   `similaritySearch(query, topK)` 로 코사인 유사도가 높은 청크를 돌려주는 검색 인덱스.
 *
 * - **`SimpleVectorStore`** — Spring AI 가 제공하는 **인메모리** 구현체.
 *   운영에서는 PgVector / Redis Stack / Chroma / Pinecone 등으로 교체한다.
 *   본 PoC 는 Docker 의존을 줄이기 위해 인메모리만 사용.
 */
@Configuration
class VectorStoreConfig {

    @Bean
    fun vectorStore(embeddingModel: EmbeddingModel): VectorStore =
        SimpleVectorStore.builder(embeddingModel).build()
}
