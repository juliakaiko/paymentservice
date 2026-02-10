package com.mymicroservice.paymentservice.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GatewayAuthFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String INTERNAL_CALL_HEADER = "X-Internal-Call";
    private static final String SOURCE_SERVICE_HEADER = "X-Source-Service";
    private static final String GATEWAY_SERVICE_NAME = "gateway";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        try {
            if (isGatewayCall(request)) {
                log.info("Request received from Gateway, processing JWT authentication");
                parseJwtAndAuthenticate(request);
            } else {
                SecurityContextHolder.clearContext();
                log.info("Internal service-to-service call detected, no authentication required");
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            log.error("Authentication processing failed: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Define calls from Gateway by the X-Internal-Call and X-Source-Service header
     */
    private boolean isGatewayCall(HttpServletRequest request) {
        String internalCall = request.getHeader(INTERNAL_CALL_HEADER);
        String sourceService = request.getHeader(SOURCE_SERVICE_HEADER);

        boolean result = Boolean.parseBoolean(internalCall)
                && GATEWAY_SERVICE_NAME.equalsIgnoreCase(sourceService);

        log.debug("isGatewayCall check: X-Internal-Call={}, result={}", internalCall, result);
        return result;
    }

    /**
     * JWT parsing and SecurityContext installation
     */
    private void parseJwtAndAuthenticate(HttpServletRequest request) throws IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("No Bearer token found in request from Gateway");
            return;
        }

        String token = authHeader.substring(7);
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            log.warn("Invalid JWT structure, skipping authentication");
            return;
        }

        /**
         * JWT Base64 parsing, without signature verification (Gateway does it)
         */
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);

        String userId = (String) claims.get("sub");
        List<String> roles = (List<String>) claims.getOrDefault("roles", List.of());

        var authorities = roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();

        var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);

        log.info("SecurityContext set for user: {} with roles: {}", userId, roles);
    }

}
