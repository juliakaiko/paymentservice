package com.mymicroservices.paymentservice.util;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MongoLiquibaseRunner implements ApplicationRunner {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${liquibase.change-log:db/changelog/db.changelog-master.xml}")
    private String changeLog;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var resourceAccessor = new ClassLoaderResourceAccessor();

        // Открываем Mongo Liquibase Database через фабрику
        var database = (MongoLiquibaseDatabase) DatabaseFactory.getInstance()
                .openDatabase(mongoUri, null, null, null, resourceAccessor);

        try (database) {
            var liquibase = new Liquibase(changeLog, resourceAccessor, database);

            // Привяжем контексты, чтобы, например, сиды шли только в dev
            var contexts = new Contexts(
                    (activeProfiles != null && activeProfiles.contains("dev")) ? "dev" : "");

            liquibase.update(contexts, new LabelExpression());
        }
    }
}
