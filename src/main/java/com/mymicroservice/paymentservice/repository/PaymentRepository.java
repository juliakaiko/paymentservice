package com.mymicroservice.paymentservice.repository;

import com.mymicroservice.paymentservice.model.PaymentEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends MongoRepository<PaymentEntity, String> {

    List<PaymentEntity> findByOrderId(String orderId);

    List<PaymentEntity> findByUserId(String userId);

    List<PaymentEntity> findByStatusIn(List<String> statuses);

    List<PaymentEntity> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
