package com.ginkgooai.core.identity.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class GlobalLoggingFilter extends OncePerRequestFilter {
    private static final List<String> JSON_CONTENT_TYPES = Arrays.asList(
        "application/json",
        "application/json;charset=UTF-8",
        "application/json;charset=utf-8"
    );

    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
        "/actuator",
        "/swagger",
        "/v3/api-docs"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDE_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) 
            throws ServletException, IOException {
        
        if (!(request instanceof ContentCachingRequestWrapper)) {
            request = new ContentCachingRequestWrapper(request);
        }
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        long startTime = System.currentTimeMillis();
        
        try {
            logRequest((ContentCachingRequestWrapper) request);
            chain.doFilter(request, responseWrapper);
        } finally {
            logResponse(responseWrapper, System.currentTimeMillis() - startTime);
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) throws IOException {
        log.info("=========================== Request Start ===========================");
        log.info("URI: {} {}", request.getMethod(), request.getRequestURI());
        
        String queryString = request.getQueryString();
        if (queryString != null) {
            log.info("Query String: {}", queryString);
        }
        
        Collections.list(request.getHeaderNames()).forEach(headerName -> 
            log.info("Header {}: {}", headerName, request.getHeader(headerName)));
        
        String contentType = request.getContentType();
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            String contentBody = new String(content, request.getCharacterEncoding());
            if (isJsonContent(contentType)) {
                log.info("Request Body (JSON): {}", formatJson(contentBody));
            } else if (contentType != null && contentType.contains("form")) {
                log.info("Request Body (Form): {}", formatFormData(contentBody));
            } else {
                log.info("Request Body: {}", contentBody);
            }
        }
    }

    private void logResponse(ContentCachingResponseWrapper response, long timeElapsed) throws IOException {
        String contentType = response.getContentType();
        byte[] content = response.getContentAsByteArray();
        
        log.info("Response Status: {}", response.getStatus());
        log.info("Time Elapsed: {}ms", timeElapsed);
        
        // 记录响应头
        response.getHeaderNames().forEach(headerName -> 
            log.info("Response Header {}: {}", headerName, response.getHeader(headerName)));
        
        if (content.length > 0) {
            String responseBody = new String(content, response.getCharacterEncoding());
            if (isJsonContent(contentType)) {
                log.info("Response Body (JSON): {}", formatJson(responseBody));
            } else {
                log.info("Response Body: {}", responseBody);
            }
        }
        
        log.info("=========================== Request End ===========================\n");
    }

    private boolean isJsonContent(String contentType) {
        if (contentType == null) return false;
        String lowerContentType = contentType.toLowerCase();
        return JSON_CONTENT_TYPES.stream()
            .anyMatch(lowerContentType::contains);
    }

    private String formatJson(String content) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object json = mapper.readValue(content, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            return content;
        }
    }

    private String formatFormData(String content) {
        try {
            StringBuilder formatted = new StringBuilder();
            String[] pairs = content.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name());
                String value = keyValue.length > 1 ? 
                    URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name()) : "";
                formatted.append(key).append(" = ").append(value).append("\n");
            }
            return formatted.toString();
        } catch (Exception e) {
            return content;
        }
    }
}
