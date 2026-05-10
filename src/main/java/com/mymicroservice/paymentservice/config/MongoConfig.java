package com.mymicroservice.paymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

/*
    Mongo транзакции работают только в replica set (или sharded cluster через mongos)
    и не поддерживаются в standalone режиме MongoDB.
    Данную конфигурацию включать при запуске из Docker.
*/
//@Configuration
public class MongoConfig {

    //@Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
