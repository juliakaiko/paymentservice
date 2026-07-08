package com.mymicroservice.paymentservice.configuration;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class LiquibaseTestOverride {

    @Bean
    @Primary
    SpringLiquibase liquibase() {
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
