package com.ginkgooai.core.identity.filter;

import com.ginkgooai.core.identity.service.MfaAuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MfaAuthenticationFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;
    private final MfaAuthenticationService mfaAuthenticationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!isLoginRequest(request)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String email = request.getParameter("email");
            String mfaCode = request.getParameter("mfaCode");

            Map<String, Object> result = mfaAuthenticationService.handleMfa(email, mfaCode);

            if (result.containsKey("requireMfa")) {
                writeJsonResponse(response, HttpStatus.UNAUTHORIZED, result);
                return;
            }

            chain.doFilter(request, response);

        } catch (Exception e) {
            handleAuthenticationError(response, e);
        }
    }

    private void writeJsonResponse(HttpServletResponse response,
                                   HttpStatus status,
                                   Map<String, Object> body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private void handleAuthenticationError(HttpServletResponse response, Exception e) throws IOException {
        Map<String, Object> error = Map.of("error", e.getMessage());
        writeJsonResponse(response, HttpStatus.UNAUTHORIZED, error);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return request.getMethod().equals("POST") && request.getRequestURI().equals("/login");
    }
}