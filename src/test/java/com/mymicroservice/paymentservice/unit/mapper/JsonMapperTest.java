package com.mymicroservice.paymentservice.unit.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymicroservice.paymentservice.mapper.JsonMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mymicroservices.common.events.OrderEventDto;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonMapperTest {

    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        jsonMapper = new JsonMapper(new ObjectMapper(), validator);
    }

    @Test
    void toJson_ShouldReturnEmpty_WhenObjectIsNull() {
        assertTrue(jsonMapper.toJson(null).isEmpty());
    }

    @Test
    void toJson_ShouldSerializeObject_WhenObjectIsValid() {
        OrderEventDto dto = OrderEventDto.builder()
                .orderId("1")
                .userId("1")
                .paymentAmount(BigDecimal.TEN)
                .build();

        Optional<String> result = jsonMapper.toJson(dto);

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("\"orderId\":\"1\""));
    }

    @Test
    void fromJson_ShouldReturnEmpty_WhenJsonIsBlank() {
        assertTrue(jsonMapper.fromJson(" ", OrderEventDto.class).isEmpty());
    }

    @Test
    void fromJson_ShouldDeserializeObject_WhenJsonIsValid() {
        String json = "{\"orderId\":\"1\",\"userId\":\"1\",\"paymentAmount\":10}";

        Optional<OrderEventDto> result = jsonMapper.fromJson(json, OrderEventDto.class);

        assertTrue(result.isPresent());
    }

    @Test
    void fromJson_ShouldReturnEmpty_WhenJsonIsInvalid() {
        assertFalse(jsonMapper.fromJson("{invalid", OrderEventDto.class).isPresent());
    }

    @Test
    void fromJson_ShouldReturnEmpty_WhenBytesAreNull() {
        assertTrue(jsonMapper.fromJson((byte[]) null, OrderEventDto.class).isEmpty());
    }

    @Test
    void fromJson_ShouldReturnEmpty_WhenBytesAreEmpty() {
        assertTrue(jsonMapper.fromJson(new byte[0], OrderEventDto.class).isEmpty());
    }

    @Test
    void fromJson_ShouldDeserializeFromBytes_WhenJsonIsValid() {
        byte[] json = "{\"orderId\":\"1\",\"userId\":\"1\",\"paymentAmount\":10}".getBytes();

        assertTrue(jsonMapper.fromJson(json, OrderEventDto.class).isPresent());
    }

    @Test
    void toJson_ShouldReturnEmpty_WhenSerializationFails() {
        assertTrue(jsonMapper.toJson(new UnserializableObject()).isEmpty());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void fromJson_ShouldReturnEmpty_WhenValidationFails() {
        jakarta.validation.Validator validator = org.mockito.Mockito.mock(jakarta.validation.Validator.class);
        jakarta.validation.ConstraintViolation<OrderEventDto> violation =
                org.mockito.Mockito.mock(jakarta.validation.ConstraintViolation.class);
        jakarta.validation.Path path = org.mockito.Mockito.mock(jakarta.validation.Path.class);
        org.mockito.Mockito.when(path.toString()).thenReturn("orderId");
        org.mockito.Mockito.when(violation.getPropertyPath()).thenReturn(path);
        java.util.Set violations = java.util.Set.of(violation);
        org.mockito.Mockito.when(validator.validate(org.mockito.ArgumentMatchers.any())).thenReturn(violations);
        JsonMapper mapperWithMock = new JsonMapper(new ObjectMapper(), validator);

        assertTrue(mapperWithMock.fromJson(
                "{\"orderId\":\"1\",\"userId\":\"1\",\"paymentAmount\":10}", OrderEventDto.class).isEmpty());
    }

    @Test
    void fromJson_ShouldReturnEmpty_WhenUnexpectedExceptionOccurs() throws Exception {
        ObjectMapper failingMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        org.mockito.Mockito.when(failingMapper.readValue(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(OrderEventDto.class)))
                .thenThrow(new RuntimeException("unexpected"));

        JsonMapper mapperWithMock = new JsonMapper(failingMapper,
                Validation.buildDefaultValidatorFactory().getValidator());

        assertTrue(mapperWithMock.fromJson("{\"orderId\":\"1\"}", OrderEventDto.class).isEmpty());
    }

    @Test
    void fromJson_ShouldReturnEmpty_WhenBytesAreInvalid() {
        assertTrue(jsonMapper.fromJson("{invalid".getBytes(), OrderEventDto.class).isEmpty());
    }

    @Test
    void fromJson_ShouldReturnEmpty_WhenByteArrayUnexpectedExceptionOccurs() throws Exception {
        ObjectMapper failingMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        org.mockito.Mockito.when(failingMapper.readValue(
                org.mockito.ArgumentMatchers.any(byte[].class),
                org.mockito.ArgumentMatchers.eq(OrderEventDto.class)))
                .thenThrow(new RuntimeException("unexpected"));

        JsonMapper mapperWithMock = new JsonMapper(failingMapper,
                Validation.buildDefaultValidatorFactory().getValidator());

        assertTrue(mapperWithMock.fromJson("{\"orderId\":\"1\"}".getBytes(), OrderEventDto.class).isEmpty());
    }

    @Test
    void toJson_ShouldReturnEmpty_WhenUnexpectedSerializationExceptionOccurs() throws Exception {
        ObjectMapper failingMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        org.mockito.Mockito.when(failingMapper.writeValueAsString(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("unexpected"));

        JsonMapper mapperWithMock = new JsonMapper(failingMapper,
                Validation.buildDefaultValidatorFactory().getValidator());

        assertTrue(mapperWithMock.toJson(OrderEventDto.builder().orderId("1").build()).isEmpty());
    }

    private static class UnserializableObject {
        public UnserializableObject getSelf() {
            return this;
        }
    }
}
