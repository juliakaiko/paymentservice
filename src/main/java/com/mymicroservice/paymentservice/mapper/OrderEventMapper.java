package com.mymicroservice.paymentservice.mapper;

import com.mymicroservice.paymentservice.model.PaymentEntity;
import org.mymicroservices.common.events.OrderEventDto;
import lombok.NonNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface OrderEventMapper {

    OrderEventMapper INSTANCE = Mappers.getMapper(OrderEventMapper.class);

    @Mapping(target = "orderId", source = "orderEventDto.orderId")
    @Mapping(target = "userId", source = "orderEventDto.userId")
    @Mapping(target = "timestamp", expression = "java(java.time.LocalDateTime.now().withNano(0))")
    @Mapping(target = "paymentAmount", source = "orderEventDto.paymentAmount")
    PaymentEntity toEntity (@NonNull OrderEventDto orderEventDto);
}
