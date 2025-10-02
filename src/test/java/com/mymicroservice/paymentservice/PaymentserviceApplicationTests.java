package com.mymicroservice.paymentservice;

import com.mymicroservice.paymentservice.config.MongoTestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PaymentserviceApplicationTests extends MongoTestcontainersConfig{

	@Test
	void contextLoads() {
	}

}
