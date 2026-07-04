package com.biuea.chat.infrastructure.config

import com.biuea.chat.infrastructure.redis.RedisMessageBroker
import com.biuea.chat.infrastructure.redis.ServerChannelSubscriber
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer

@Configuration
class RedisConfig {
    @Bean
    fun serverChannelContainer(
        connectionFactory: RedisConnectionFactory,
        subscriber: ServerChannelSubscriber,
        @Value("\${server.id}") serverId: String,
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)
        container.addMessageListener(subscriber, ChannelTopic(RedisMessageBroker.channel(serverId)))
        return container
    }
}
