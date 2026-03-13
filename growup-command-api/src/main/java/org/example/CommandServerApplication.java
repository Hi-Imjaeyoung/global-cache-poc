package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@EntityScan(basePackages = {"org.example.domain"})
@EnableJpaRepositories(basePackages = {"org.example.repo"})
public class CommandServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommandServerApplication.class, args);
    }

}