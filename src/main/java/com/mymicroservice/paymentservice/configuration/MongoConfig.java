package com.mymicroservice.paymentservice.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

/*
    Mongo транзакции работают только в replica set (или sharded cluster через mongos)
    и не поддерживаются в standalone режиме MongoDB.
    Данную конфигурацию включать при запуске из Docker.
*/
@Configuration
@Profile("prod")
@Slf4j
public class MongoConfig {

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        log.info("Initializing MongoTransactionManager for PROD environment (replica set mode)");
        return new MongoTransactionManager(dbFactory);
    }
}
