package org.example.config;

import org.example.listener.L1CacheEvictSubscriber;
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

    @Bean
    public ChannelTopic cacheEvictTopic() {
        return new ChannelTopic(L1_CACHE_EVICT_TOPIC);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListener(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter,
            ChannelTopic cacheEvictTopic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, cacheEvictTopic);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // 평소엔 2명만 대기
        executor.setMaxPoolSize(5);  // 바빠도 최대 5명까지만 일해!
        executor.setThreadNamePrefix("Redis-Sub-");
        executor.initialize();
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(L1CacheEvictSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }
}