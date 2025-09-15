package com.mymicroservice.paymentservice;

import com.mymicroservice.paymentservice.config.MongoTestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

//@Import(MongoTestcontainersConfig.class)
@SpringBootTest
@ActiveProfiles("test")
class PaymentserviceApplicationTests extends MongoTestcontainersConfig{

	@Test
	void contextLoads() {
	}

}
