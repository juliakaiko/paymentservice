package com.mymicroservice.paymentservice.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonMapper {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    /**
     * Сериализует объект в JSON строку.
     *
     * @param obj объект для сериализации
     * @return Optional с JSON строкой или empty, если объект null или ошибка сериализации
     */
    public Optional<String> toJson(Object obj) {
        if (obj == null) {
            log.debug("Attempted to serialize null object");
            return Optional.empty();
        }
        try {
            String json = objectMapper.writeValueAsString(obj);
            log.debug("Successfully serialized object of type: {}", obj.getClass().getSimpleName());
            return Optional.of(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object of type: {}. Error type: {}",
                    obj.getClass().getSimpleName(),
                    e.getClass().getSimpleName());
            return Optional.empty();
        }
        catch (Exception e) {
            log.error("Unexpected error during Object deserialization for class: {}. Error: {}",
                    obj.getClass().getSimpleName(),
                    e.getClass().getSimpleName()
            );
            return Optional.empty();
        }
    }

    /**
     * Десериализует JSON строку в объект с валидацией.
     *
     * @param json  JSON строка
     * @param clazz класс целевого объекта
     * @return Optional с десериализованным объектом или empty при ошибке
     */
    public <T> Optional<T> fromJson(String json, Class<T> clazz) {
        if (!StringUtils.hasText(json)) {
            log.debug("Attempted to deserialize empty or null JSON string");
            return Optional.empty();
        }
        try {
            T value = objectMapper.readValue(json, clazz);
            return validateAndReturn(value, clazz);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize JSON to class {}. Error type: {}",
                    clazz.getSimpleName(),
                    e.getClass().getSimpleName()
            );
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error during JSON deserialization for class: {}. Error: {}",
                    clazz.getSimpleName(),
                    e.getClass().getSimpleName()
            );
            return Optional.empty();
        }
    }

    /**
     * Десериализует байтовый массив в объект с валидацией.
     *
     * @param bytes байтовый массив JSON
     * @param clazz класс целевого объекта
     * @return Optional с десериализованным объектом или empty при ошибке
     */
    public <T> Optional<T> fromJson(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            log.debug("Attempted to deserialize empty or null byte array");
            return Optional.empty();
        }
        try {
            T value = objectMapper.readValue(bytes, clazz);
            return validateAndReturn(value, clazz);
        } catch (IOException e) {
            log.warn("Failed to deserialize byte array to class {}. Error type: {}",
                    clazz.getSimpleName(),
                    e.getClass().getSimpleName()
            );
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error during byte array deserialization for class: {}. Error type: {}",
                    clazz.getSimpleName(),
                    e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /**
     * Валидирует объект и возвращает его в Optional.
     *
     * @param value объект для валидации
     * @param clazz класс объекта (для логирования)
     * @return Optional с объектом или empty при ошибках валидации
     */
    private <T> Optional<T> validateAndReturn(T value, Class<T> clazz) {
        Set<ConstraintViolation<T>> violations = validator.validate(value);

        if (!violations.isEmpty()) {
            log.warn("Validation failed. Invalid fields={}",
                    violations.stream()
                            .map(v -> v.getPropertyPath().toString())
                            .toList()
            );
            return Optional.empty();
        }

        log.debug("Successfully deserialized and validated object of type: {}", clazz.getSimpleName());
        return Optional.of(value);
    }
}
