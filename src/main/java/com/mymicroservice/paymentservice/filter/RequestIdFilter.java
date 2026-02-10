package com.mymicroservice.paymentservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";

    @Value("${spring.application.name}")
    private String serviceName;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestId = Optional.ofNullable(request.getHeader("X-Request-Id"))
                .orElse(UUID.randomUUID().toString());

        MDC.put(REQUEST_ID, requestId);
        MDC.put("serviceName", serviceName);

        response.setHeader("X-Request-Id", requestId);

        // Пишем в файл трассировки лог в начале запроса
        log.info("{} {}",
                request.getMethod(),
                request.getRequestURI());

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Пишем в файл трассировки лог при RESPONSE
            log.info("Response status: {}", response.getStatus());

            MDC.clear();
        }
    }
}
