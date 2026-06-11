package com.mymicroservice.paymentservice.model;

import com.mymicroservice.paymentservice.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    private String id;

    @Field("order_id")
    private String orderId;

    @Field("user_id")
    private String userId;

    private PaymentStatus status;

    private LocalDateTime timestamp;

    @Field("payment_amount")
    private BigDecimal paymentAmount;

}
