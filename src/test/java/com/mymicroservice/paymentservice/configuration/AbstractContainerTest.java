package com.mymicroservice.paymentservice.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("testcontainer")
@Slf4j
public class AbstractContainerTest {

    public static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine") //postgres:15
            .withDatabaseName("testdb")
            .withUsername("user")
            .withPassword("password")
            .waitingFor(Wait.forListeningPort()
                    .withStartupTimeout(Duration.ofSeconds(120))
            );

    static {
        postgreSQLContainer.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String jdbcUrl = postgreSQLContainer.getJdbcUrl();
        log.info("PostgreSQL URL: {}", jdbcUrl);
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");

        registry.add("spring.liquibase.enabled", () -> "false");
    }
}
