package org.example.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisPubSubConfig {

    public static final String L1_CACHE_EVICT_TOPIC = "l1-cache-evict-topic";
    public static final String UPDATE_CACHE_DATA = "update-cache-data";
}
