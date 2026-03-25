package org.example.config;

import com.fasterxml.jackson.core.JsonParseException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);
        // 1초 대기, 2번 재시도
        FixedBackOff backOff = new FixedBackOff(1000L, 2L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        // 재시도 하지 않음.
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class,
                JsonParseException.class
        );
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
