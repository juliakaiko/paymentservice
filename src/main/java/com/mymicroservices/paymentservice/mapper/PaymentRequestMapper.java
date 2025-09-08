package com.mymicroservices.paymentservice.mapper;

import com.mymicroservices.paymentservice.dto.PaymentRequestDto;
import com.mymicroservices.paymentservice.model.PaymentEntity;
import lombok.NonNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PaymentRequestMapper {

    PaymentRequestMapper INSTANSE = Mappers.getMapper(PaymentRequestMapper.class);

    @Mapping(target = "orderId", source = "paymentDto.orderId")
    @Mapping(target = "userId", source = "paymentDto.userId")
    @Mapping(target = "timestamp", expression = "java(java.time.LocalDateTime.now().withNano(0))")
    @Mapping(target = "paymentAmount", source = "paymentDto.paymentAmount")
    PaymentEntity toEntity (@NonNull PaymentRequestDto paymentDto);
}
