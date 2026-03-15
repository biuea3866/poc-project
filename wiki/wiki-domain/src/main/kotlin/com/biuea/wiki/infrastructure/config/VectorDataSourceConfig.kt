package com.biuea.wiki.infrastructure.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.simple.JdbcClient
import javax.sql.DataSource

@Configuration
class VectorDataSourceConfig {

    @Bean
    fun vectorDataSource(env: Environment): DataSource {
        return DataSourceBuilder.create()
            .url(env.getRequiredProperty("datasource-vector.url"))
            .username(env.getRequiredProperty("datasource-vector.username"))
            .password(env.getRequiredProperty("datasource-vector.password"))
            .driverClassName(env.getProperty("datasource-vector.driver-class-name", "org.postgresql.Driver"))
            .build()
    }

    @Bean
    fun vectorJdbcClient(
        @Qualifier("vectorDataSource") dataSource: DataSource,
    ): JdbcClient {
        return JdbcClient.create(dataSource)
    }
}
