package com.example.order

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.util.concurrent.TimeUnit

@Configuration
class HttpClientConfig(
    @Value("\${downstream.payment.base-url}") private val paymentBaseUrl: String,
    @Value("\${downstream.inventory.base-url}") private val inventoryBaseUrl: String,
) {

    private fun lowLevelClient(): HttpClient =
        HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1_000)
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(10, TimeUnit.SECONDS))
                conn.addHandlerLast(WriteTimeoutHandler(10, TimeUnit.SECONDS))
            }

    @Bean("paymentWebClient")
    fun paymentWebClient(): WebClient = WebClient.builder()
        .baseUrl(paymentBaseUrl)
        .clientConnector(ReactorClientHttpConnector(lowLevelClient()))
        .build()

    @Bean("inventoryWebClient")
    fun inventoryWebClient(): WebClient = WebClient.builder()
        .baseUrl(inventoryBaseUrl)
        .clientConnector(ReactorClientHttpConnector(lowLevelClient()))
        .build()
}
