package com.mymicroservice.paymentservice.unit.filter;

import com.mymicroservice.paymentservice.filter.GatewayAuthFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static com.mymicroservice.paymentservice.util.CommonConstants.GATEWAY_SERVICE_NAME;
import static com.mymicroservice.paymentservice.util.CommonConstants.INTERNAL_CALL_HEADER;
import static com.mymicroservice.paymentservice.util.CommonConstants.SOURCE_SERVICE_HEADER;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GatewayAuthFilterTest {

    @InjectMocks
    private GatewayAuthFilter gatewayAuthFilter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        ReflectionTestUtils.setField(gatewayAuthFilter, "publicEndpoints", List.of("/actuator/**"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_ShouldAuthenticateUser_WhenGatewayHeadersAndJwtAreValid() throws Exception {
        request.addHeader(INTERNAL_CALL_HEADER, "true");
        request.addHeader(SOURCE_SERVICE_HEADER, GATEWAY_SERVICE_NAME);
        request.addHeader("Authorization", "Bearer " + buildJwt("user-1", List.of("USER")));

        gatewayAuthFilter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldClearSecurityContext_WhenInternalServiceCallDetected() throws Exception {
        gatewayAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldClearSecurityContext_WhenBearerTokenIsMissing() throws Exception {
        request.addHeader(INTERNAL_CALL_HEADER, "true");
        request.addHeader(SOURCE_SERVICE_HEADER, GATEWAY_SERVICE_NAME);

        gatewayAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldClearSecurityContext_WhenJwtStructureIsInvalid() throws Exception {
        request.addHeader(INTERNAL_CALL_HEADER, "true");
        request.addHeader(SOURCE_SERVICE_HEADER, GATEWAY_SERVICE_NAME);
        request.addHeader("Authorization", "Bearer invalid-token");

        gatewayAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldClearSecurityContext_WhenJwtPayloadIsMalformed() throws Exception {
        request.addHeader(INTERNAL_CALL_HEADER, "true");
        request.addHeader(SOURCE_SERVICE_HEADER, GATEWAY_SERVICE_NAME);
        request.addHeader("Authorization", "Bearer header.not-json-payload.signature");

        gatewayAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldClearSecurityContext_WhenGatewaySourceIsInvalid() throws Exception {
        request.addHeader(INTERNAL_CALL_HEADER, "true");
        request.addHeader(SOURCE_SERVICE_HEADER, "unknown-service");

        gatewayAuthFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    private String buildJwt(String userId, List<String> roles) {
        String payloadJson = "{\"sub\":\"" + userId + "\",\"roles\":[\"" + String.join("\",\"", roles) + "\"]}";
        return encodeJwt(payloadJson);
    }

    private String encodeJwt(String payloadJson) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".signature";
    }
}
