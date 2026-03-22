package com.closet.search.infrastructure.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories

@Configuration
@EnableElasticsearchRepositories(basePackages = ["com.closet.search.infrastructure.repository"])
class ElasticsearchConfig
