package com.mymicroservice.paymentservice.configuration;

import com.mymicroservice.paymentservice.util.PaymentDataLoader;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Базовый класс для Kafka integration-тестов:
 * PostgreSQL (Testcontainers) + MongoDB (Testcontainers) + EmbeddedKafka.
 */
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles({"testcontainer", "dev"})
@Import(AbstractKafkaIntegrationTest.LiquibaseDisabledConfig.class)
public abstract class AbstractKafkaIntegrationTest extends AbstractContainerTest {

    @Container
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @MockBean
    protected PaymentDataLoader paymentDataLoader;

    @DynamicPropertySource
    static void integrationProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @TestConfiguration
    static class LiquibaseDisabledConfig {

        @Bean
        @Primary
        SpringLiquibase postgresLiquibase() {
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setShouldRun(false);
            return liquibase;
        }

        @Bean
        @Primary
        CommandLineRunner liquibaseMigrationRunner() {
            return args -> {
            };
        }
    }
}
