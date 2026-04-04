package com.closet.search.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories

/**
 * Elasticsearch 클라이언트 설정.
 *
 * Spring Data Elasticsearch를 통해 ES 8.11에 연결하며,
 * nori 플러그인이 설치된 인스턴스를 대상으로 한다.
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = ["com.closet.search.infrastructure.repository"])
class ElasticsearchConfig(
    @Value("\${spring.elasticsearch.uris:http://localhost:9200}")
    private val esUri: String,
) : ElasticsearchConfiguration() {

    override fun clientConfiguration(): ClientConfiguration {
        return ClientConfiguration.builder()
            .connectedTo(esUri.removePrefix("http://").removePrefix("https://"))
            .build()
    }
}
