package com.mymicroservice.paymentservice;

import com.mymicroservice.paymentservice.config.MongoTestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(MongoTestcontainersConfig.class)
@SpringBootTest
class PaymentserviceApplicationTests {

	@Test
	void contextLoads() {
	}

}
