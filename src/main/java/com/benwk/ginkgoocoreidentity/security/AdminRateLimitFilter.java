package com.benwk.ginkgoocoreidentity.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class AdminRateLimitFilter extends OncePerRequestFilter {
    
    private final Bucket bucket;
    private final AdminSecurityProperties properties;

    public AdminRateLimitFilter(AdminSecurityProperties properties) {
        this.properties = properties;
        
        Bandwidth limit = Bandwidth.classic(properties.getRateLimit(), 
            Refill.intervally(properties.getRateLimit(), Duration.ofSeconds(1)));
        
        this.bucket = Bucket.builder()
            .addLimit(limit)
            .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        
        if (!bucket.tryConsume(1)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\": \"Too many requests\", \"message\": \"Please try again later\"}");
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