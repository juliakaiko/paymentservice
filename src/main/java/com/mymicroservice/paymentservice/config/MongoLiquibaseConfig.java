package com.mymicroservice.paymentservice.config;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoLiquibaseConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${liquibase.change-log:db/changelog/db.changelog-master.xml}")
    private String changeLog;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @Bean
    public CommandLineRunner liquibaseMigrationRunner() {
        return args -> {
            var resourceAccessor = new ClassLoaderResourceAccessor();
            var database = (MongoLiquibaseDatabase) DatabaseFactory.getInstance()
                    .openDatabase(mongoUri, null, null, null, resourceAccessor);

            try (database) {
                var liquibase = new Liquibase(changeLog, resourceAccessor, database);

                // We define the context depending on the profile
                String contextName = "";
                if (activeProfiles != null && activeProfiles.contains("dev")) {
                    contextName = "dev";
                } else if (activeProfiles != null && activeProfiles.contains("prod")) {
                    contextName = "prod";
                }

                var contexts = new Contexts(contextName);
                liquibase.update(contexts, new LabelExpression());

                System.out.println("Liquibase migration completed successfully for context: " + contextName);
            }
        };
    }
}
