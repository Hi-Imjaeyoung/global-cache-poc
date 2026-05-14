package org.example.config;

import org.example.listener.L1CacheEvictSubscriber;
import org.example.listener.UpdateEventSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class RedisPubSubConfig {

    public static final String L1_CACHE_EVICT_TOPIC = "l1-cache-evict-topic";
    public static final String UPDATE_CACHE_DATA = "update-cache-data";


    @Bean
    public RedisMessageListenerContainer redisMessageListener(
            RedisConnectionFactory connectionFactory,
            L1CacheEvictSubscriber l1CacheEvictSubscriber,
            UpdateEventSubscriber updateEventSubscriber)
    {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(redisSubExecutor());
        container.addMessageListener(l1CacheEvictSubscriber, new ChannelTopic(L1_CACHE_EVICT_TOPIC));
        container.addMessageListener(updateEventSubscriber, new ChannelTopic(UPDATE_CACHE_DATA));
        return container;
    }

    @Bean
    public ThreadPoolTaskExecutor redisSubExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setThreadNamePrefix("Redis-Sub-");
        executor.initialize();
        return executor;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(L1CacheEvictSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }
}