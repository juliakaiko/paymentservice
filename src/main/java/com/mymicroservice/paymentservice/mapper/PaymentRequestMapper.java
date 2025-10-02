package com.mymicroservice.paymentservice.mapper;

import com.mymicroservice.paymentservice.dto.PaymentRequestDto;
import lombok.NonNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.mymicroservices.common.events.OrderEventDto;

@Mapper
public interface PaymentRequestMapper {

    PaymentRequestMapper INSTANCE = Mappers.getMapper(PaymentRequestMapper.class);

    @Mapping(target = "orderId", source = "paymentRequestDto.orderId")
    @Mapping(target = "userId", source = "paymentRequestDto.userId")
    @Mapping(target = "paymentAmount", source = "paymentRequestDto.paymentAmount")
    OrderEventDto toOrderEventDto(@NonNull PaymentRequestDto paymentRequestDto);
}
