package org.example.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // 1. 상수 정의 (실무에선 관리 편하게 상수로 빼두는 게 국룰)
    public static final String EXCHANGE_NAME = "tree.exchange";
    public static final String QUEUE_NAME = "query.tree.queue";
    public static final String ROUTING_KEY = "tree.#"; // order로 시작하는 모든 이벤트 라우팅 (*.order.# 등도 가능)

    /**
     * 2. 메시지 컨버터 설정 (JSON 직렬화/역직렬화)
     * 이 빈(Bean)을 등록해두면 스프링 부트가 알아서 두 가지를 처리해줘.
     * - Command 서버가 RabbitTemplate으로 메시지 보낼 때 자동으로 JSON 변환
     * - Query 서버가 @RabbitListener로 메시지 받을 때 자동으로 Java 객체 변환
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 3. 토픽 익스체인지(Exchange) 선언
     * 프로듀서(Command)가 이 익스체인지 이름을 지정해서 이벤트를 던지게 돼.
     */
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    /**
     * 4. 큐(Queue) 선언
     * 소비자(Query)가 이 큐를 리스닝하면서 이벤트를 하나씩 쏙쏙 빼먹게 돼.
     */
    @Bean
    public Queue queryOrderQueue() {
        // 첫 번째 파라미터는 큐 이름
        // 두 번째 파라미터(true)는 Durable 설정 -> 래밋MQ가 죽었다 깨어나도 이 큐를 유지함
        return new Queue(QUEUE_NAME, true);
    }

    /**
     * 5. 바인딩(Binding) 설정
     * 우체국(Exchange)과 우편함(Queue)을 라우팅 키(주소)로 묶어주는 끈이야.
     */
    @Bean
    public Binding orderBinding(Queue queryOrderQueue, TopicExchange orderExchange) {
        return BindingBuilder
                .bind(queryOrderQueue)
                .to(orderExchange)
                .with(ROUTING_KEY);
    }
}
