package com.ginkgooai.core.identity.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public class ProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {
    @Value("${AUTH_SERVER}")
    private String authServer;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        handleException(request, response, authException, HttpStatus.UNAUTHORIZED, "unauthorized");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        handleException(request, response, accessDeniedException, HttpStatus.FORBIDDEN, "forbidden");
    }

    private void handleException(HttpServletRequest request, HttpServletResponse response,
            Exception exception, HttpStatus status, String errorType) throws IOException {
        ProblemDetail problemDetail = ProblemDetail
                .forStatusAndDetail(status, exception.getMessage());
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setType(URI.create(authServer + "/errors/" + errorType));
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        new ObjectMapper().writeValue(response.getOutputStream(), problemDetail);
    }
}