package com.mymicroservice.paymentservice.unit.security;

import com.mymicroservice.paymentservice.security.CustomAccessDeniedHandler;
import com.mymicroservice.paymentservice.security.CustomAuthenticationEntryPoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityHandlersTest {

    @Test
    void commence_ShouldReturnUnauthorizedJson_WhenAuthenticationFails() throws Exception {
        CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        entryPoint.commence(request, response, new BadCredentialsException("bad creds"));

        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        assertTrue(writer.toString().contains("Authentication required"));
    }

    @Test
    void handle_ShouldReturnForbiddenJson_WhenAccessDenied() throws Exception {
        CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        handler.handle(request, response, new AccessDeniedException("denied"));

        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        assertTrue(writer.toString().contains("Forbidden"));
    }
}
