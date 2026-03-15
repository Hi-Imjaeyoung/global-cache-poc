package org.example.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryDslConfig {

    // 스프링이 관리하는 영속성 컨텍스트(EntityManager)를 주입받아!
    @PersistenceContext
    private EntityManager entityManager;

    // "스프링아, 앞으로 누가 JPAQueryFactory 달라고 하면 이거 만들어서 줘!"
    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}