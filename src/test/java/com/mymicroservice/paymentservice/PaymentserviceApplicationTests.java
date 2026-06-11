package com.mymicroservice.paymentservice;

import com.mymicroservice.paymentservice.config.MongoTestcontainersConfig;
import com.mymicroservice.paymentservice.kafka.PaymentEventProducer;
import com.mymicroservice.paymentservice.kafka.inbox.InboxEventConsumer;
import com.mymicroservice.paymentservice.metrics.InboxMetrics;
import com.mymicroservice.paymentservice.scheduler.InboxScheduler;
import com.mymicroservice.paymentservice.service.impl.InboxServiceImpl;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"test", "dev"})
@Import(PaymentserviceApplicationTests.LiquibaseTestOverride.class)
class PaymentserviceApplicationTests extends MongoTestcontainersConfig {

    @MockBean
    private PaymentEventProducer paymentEventProducer;

    @MockBean
    private InboxEventConsumer inboxEventConsumer;

    @MockBean
    private InboxScheduler inboxScheduler;

    @MockBean
    private InboxServiceImpl inboxService;

    @MockBean
    private InboxMetrics inboxMetrics;

    @Test
    void contextLoads_ShouldStartApplicationContext_WhenTestProfileActive() {
    }

    @TestConfiguration
    static class LiquibaseTestOverride {

        @Bean
        @Primary
        SpringLiquibase postgresLiquibase() {
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setShouldRun(false);
            return liquibase;
        }

        @Bean
        @Primary
        InitializingBean mongoLiquibase() {
            return () -> {
            };
        }

        @Bean
        @Primary
        CommandLineRunner liquibaseMigrationRunner() {
            return args -> {
            };
        }
    }
}
