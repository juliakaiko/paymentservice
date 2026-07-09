package com.mymicroservice.paymentservice;

import com.mymicroservice.paymentservice.configuration.LiquibaseTestOverride;
import com.mymicroservice.paymentservice.configuration.MongoTestcontainersConfig;
import com.mymicroservice.paymentservice.kafka.PaymentEventProducer;
import com.mymicroservice.paymentservice.kafka.inbox.InboxEventConsumer;
import com.mymicroservice.paymentservice.metrics.InboxMetrics;
import com.mymicroservice.paymentservice.scheduler.InboxScheduler;
import com.mymicroservice.paymentservice.service.impl.InboxServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"test", "dev"})
@Import(LiquibaseTestOverride.class)
class PaymentserviceApplicationTests extends MongoTestcontainersConfig {

    @MockBean
    private PaymentEventProducer paymentEventProducer;

    @MockBean
    private InboxEventConsumer inboxEventConsumer;

    @MockBean
    private InboxScheduler inboxScheduler;

    @MockBean
    private InboxServiceImpl inboxService;

    @MockBean
    private InboxMetrics inboxMetrics;

    @Test
    void contextLoads_ShouldStartApplicationContext_WhenTestProfileActive() {
    }
}
