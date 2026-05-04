package org.example.common.config;

import org.example.common.GenericBulkRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@TestConfiguration
public class TestDataConfig {
    @Bean
    public GenericBulkRepository genericBulkRepository(JdbcTemplate jdbcTemplate) {
        return new GenericBulkRepository(jdbcTemplate);
    }

}
