package com.ginkgooai.core.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AdminApiKeyFilter extends OncePerRequestFilter {
    
    private final AdminSecurityProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        
        String apiKey = request.getHeader("X-API-Key");
        
        if (!StringUtils.hasText(apiKey) || !apiKey.equals(properties.getApiKey())) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid API key");
            return;
        }
        
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return !path.startsWith("/api/v1/oauth2/");
    }

}