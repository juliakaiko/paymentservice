package com.mymicroservice.paymentservice.config;

import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.ext.mongodb.database.MongoClientDriver;
import liquibase.ext.mongodb.database.MongoConnection;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class LiquibaseConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Bean
    public SpringLiquibase postgresLiquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:db/changelog/postgresql/v.1.0/db.changelog-v.1.0.xml");
        liquibase.setShouldRun(true);
        return liquibase;
    }

    @Bean
    public InitializingBean mongoLiquibase() {
        return () -> {
            MongoConnection connection = new MongoConnection();
            connection.open(
                    mongoUri,
                    new MongoClientDriver(),
                    new Properties()
            );

            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);

            Liquibase liquibase = new Liquibase(
                    "db/changelog/mongodb/v.1.0/db.changelog-v.1.0.xml",
                    new ClassLoaderResourceAccessor(),
                    database
            );
            liquibase.update(new Contexts(), new LabelExpression());
        };
    }
}
