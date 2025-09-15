package com.mymicroservice.paymentservice.repository;

import com.mymicroservice.paymentservice.config.MongoTestcontainersConfig;
import com.mymicroservice.paymentservice.model.PaymentEntity;
import com.mymicroservice.paymentservice.model.Status;
import com.mymicroservice.paymentservice.util.PaymentEntitiesGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@Slf4j
@DataMongoTest
//@Import(MongoTestcontainersConfig.class)
public class PaymentRepositoryTest extends MongoTestcontainersConfig {

    @Autowired
    private  PaymentRepository paymentRepository;

    private List<PaymentEntity> expectedPayments;
    private final static String ENTITY_ID = "1";

    @BeforeEach
    void init() {
        expectedPayments = paymentRepository.saveAll(PaymentEntitiesGenerator.generatePaymentEntities());
    }

    @AfterEach
    void clean(){
        paymentRepository.deleteAll();
    }

    @Test
    void testFindByOrderId() {
        var result = paymentRepository.findByOrderId(ENTITY_ID);

        log.info("▶ Running test: testFindByOrderId(), OrderId={}", ENTITY_ID);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(expectedPayments.get(0));
    }

    @Test
    void testFindByUserId() {
        var result = paymentRepository.findByUserId(ENTITY_ID);

        log.info("▶ Running test: testFindByUserId(), UserId={}", ENTITY_ID);
        assertThat(result).hasSize(1);
        assertThat(result)
                .extracting(PaymentEntity::getOrderId)
                .containsExactlyInAnyOrder("1");
    }

    @Test
    void testFindByStatusIn() {
        var result = paymentRepository.findByStatusIn(List.of("PAID", "FAILED"));

        log.info("▶ Running test: testFindByStatusIn()");
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(PaymentEntity::getStatus)
                .containsExactlyInAnyOrder(Status.FAILED, Status.PAID);
    }

    @Test
    void testFindByTimestampBetween() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 1, 10, 1);
        LocalDateTime end = LocalDateTime.of(2025, 3, 3, 3, 30, 3);

        var result = paymentRepository.findByTimestampBetween(start, end);
        log.info("▶ Running test: testFindByTimestampBetween()");

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(PaymentEntity::getOrderId)
                .containsExactlyInAnyOrder("1", "2");
    }

}
