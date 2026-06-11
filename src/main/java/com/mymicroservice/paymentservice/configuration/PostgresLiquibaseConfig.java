package com.mymicroservice.paymentservice.configuration;

import liquibase.integration.spring.SpringLiquibase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class PostgresLiquibaseConfig {

    @Value("${liquibase.postgres.changelog}")
    private String postgresChangelogPath;


    @Bean
    public SpringLiquibase postgresLiquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(postgresChangelogPath);
        liquibase.setShouldRun(true);
        return liquibase;
    }
}
