package com.mymicroservice.paymentservice.repository;

import com.mymicroservice.paymentservice.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {

    Optional<Payment> findFirstByOrderId(String orderId);

    List<Payment> findByOrderId(String orderId);

    List<Payment> findByUserId(String userId);

    List<Payment> findByStatusIn(List<String> statuses);

    List<Payment> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
