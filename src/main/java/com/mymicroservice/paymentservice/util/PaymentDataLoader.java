package com.mymicroservice.paymentservice.util;

import com.mymicroservice.paymentservice.model.PaymentEntity;
import com.mymicroservice.paymentservice.model.Status;
import com.mymicroservice.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PaymentDataLoader is responsible for loading initial test payment data into MongoDB at application startup.
 * <p>
 * Each PaymentEntity has a unique <code>_id</code>. This ensures that the upsert operation is performed:
 * <ul>
 *     <li>If a document with the same <code>_id</code> exists, it is updated with the new values.</li>
 *     <li>If no document with the given <code>_id</code> exists, a new document is inserted.</li>
 * </ul>
 * <p>
 * This approach prevents duplicate entries when the application is restarted multiple times.
 */
@Component
@RequiredArgsConstructor
@Profile("!test")
public class PaymentDataLoader implements CommandLineRunner {

    private final PaymentRepository paymentRepository;

    @Override
    public void run(String... args) {
        List<PaymentEntity> payments = List.of(
                PaymentEntity.builder()
                        .id("payment-1")
                        .orderId("1")
                        .userId("1")
                        .status(Status.PAID)
                        .timestamp(LocalDateTime.of(2025, 9, 1, 17, 10, 25))
                        .paymentAmount(BigDecimal.valueOf(500000, 2))
                        .build(),
                PaymentEntity.builder()
                        .id("payment-2")
                        .orderId("2")
                        .userId("2")
                        .status(Status.FAILED)
                        .timestamp(LocalDateTime.of(2025, 9, 5, 11, 30, 1))
                        .paymentAmount(BigDecimal.valueOf(100000, 2))
                        .build()
        );

        payments.forEach(paymentRepository::save);    // Upsert по _id — MongoRepository.save() automatically updates or inserts

        System.out.println("Payment data loaded successfully!");
    }
}
