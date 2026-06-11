package com.mymicroservice.paymentservice.mapper;

import com.mymicroservice.paymentservice.model.Payment;
import lombok.NonNull;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.mymicroservices.common.events.PaymentEventDto;

@Mapper
public interface PaymentResponseMapper {

    PaymentResponseMapper INSTANCE = Mappers.getMapper(PaymentResponseMapper.class);

    @Mapping(target = "id", source = "payment.id")
    @Mapping(target = "orderId", source = "payment.orderId")
    @Mapping(target = "userId", source = "payment.userId")
    @Mapping(target = "status", source = "payment.status")
    @Mapping(target = "timestamp", source = "payment.timestamp")
    @Mapping(target = "paymentAmount", source = "payment.paymentAmount")
    PaymentEventDto toDto(Payment payment);

    /**
     * Converts {@link PaymentEventDto} back to {@link Payment} entity.
     * <p>
     * Implements <b>reverse mapping</b> relative to {@code Order -> OrderDto} conversion.
     * </p>
     *
     * @param paymentDto DTO object to convert (cannot be {@code null})
     * @return corresponding {@link Payment} entity
     */
    @InheritInverseConfiguration
    Payment toEntity (@NonNull PaymentEventDto paymentDto);
}
