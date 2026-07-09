package com.mymicroservice.paymentservice.integration.repository;

import com.mymicroservice.paymentservice.configuration.MongoTestcontainersConfig;
import com.mymicroservice.paymentservice.model.Payment;
import com.mymicroservice.paymentservice.model.enums.PaymentStatus;
import com.mymicroservice.paymentservice.repository.PaymentRepository;
import com.mymicroservice.paymentservice.util.PaymentEntitiesGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;

import java.time.LocalDateTime;
import java.util.List;

import static com.mymicroservice.paymentservice.util.data.TestConstants.ENTITY_ID;
import static com.mymicroservice.paymentservice.util.data.TestConstants.FAILED_STATUS;
import static com.mymicroservice.paymentservice.util.data.TestConstants.PAID_STATUS;
import static com.mymicroservice.paymentservice.util.data.TestConstants.SECOND_ENTITY_ID;
import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
class PaymentRepositoryTest extends MongoTestcontainersConfig {

    @Autowired
    private PaymentRepository paymentRepository;

    private List<Payment> expectedPayments;

    @BeforeEach
    void init() {
        expectedPayments = paymentRepository.saveAll(PaymentEntitiesGenerator.generatePaymentEntities());
    }

    @AfterEach
    void clean() {
        paymentRepository.deleteAll();
    }

    @Test
    void findByOrderId_ShouldReturnPayments_WhenOrderExists() {
        var result = paymentRepository.findByOrderId(ENTITY_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(expectedPayments.get(0));
    }

    @Test
    void findFirstByOrderId_ShouldReturnPayment_WhenOrderExists() {
        var result = paymentRepository.findFirstByOrderId(ENTITY_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getOrderId()).isEqualTo(ENTITY_ID);
    }

    @Test
    void findByUserId_ShouldReturnPayments_WhenUserExists() {
        var result = paymentRepository.findByUserId(ENTITY_ID);

        assertThat(result).hasSize(1);
        assertThat(result)
                .extracting(Payment::getOrderId)
                .containsExactlyInAnyOrder("1");
    }

    @Test
    void findByStatusIn_ShouldReturnPayments_WhenStatusesMatch() {
        var result = paymentRepository.findByStatusIn(List.of(PAID_STATUS, FAILED_STATUS));

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Payment::getStatus)
                .containsExactlyInAnyOrder(PaymentStatus.FAILED, PaymentStatus.PAID);
    }

    @Test
    void findByTimestampBetween_ShouldReturnPayments_WhenTimestampInRange() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 1, 10, 1);
        LocalDateTime end = LocalDateTime.of(2025, 3, 3, 3, 30, 3);

        var result = paymentRepository.findByTimestampBetween(start, end);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Payment::getOrderId)
                .containsExactlyInAnyOrder(ENTITY_ID, SECOND_ENTITY_ID);
    }
}
