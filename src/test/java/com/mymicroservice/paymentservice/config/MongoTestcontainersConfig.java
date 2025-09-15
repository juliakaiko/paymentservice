package com.mymicroservice.paymentservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@Slf4j
public class MongoTestcontainersConfig {

	@Container
	static MongoDBContainer mongoDbContainer = new MongoDBContainer("mongo:6.0");

	@DynamicPropertySource
	static void mongoProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", mongoDbContainer::getReplicaSetUrl);
	}
}
