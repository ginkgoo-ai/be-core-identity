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
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalLoggingFilter extends OncePerRequestFilter {
    private static final List<String> JSON_CONTENT_TYPES = Arrays.asList(
            "application/json",
            "application/json;charset=UTF-8",
            "application/json;charset=utf-8"
    );

    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/actuator",
            "/swagger",
            "/v3/api-docs",
            "/favicon.ico",
            "/static",
            "/webjars"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDE_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = request instanceof ContentCachingRequestWrapper ?
                (ContentCachingRequestWrapper) request : new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        try {
            chain.doFilter(requestWrapper, responseWrapper);
            logApiCall(requestWrapper, responseWrapper, System.currentTimeMillis() - startTime);
        } finally {
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logApiCall(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long timeElapsed) {
        try {
            String requestBody = getRequestBody(request);
            String responseBody = getResponseBody(response);

            log.info("API Call - {} {} - Status: {} - Time: {}ms\n-> Request: {}\n<- Response: {}",
                    request.getMethod(),
                    getFullRequestPath(request),
                    response.getStatus(),
                    timeElapsed,
                    requestBody,
                    responseBody
            );
        } catch (Exception e) {
            log.warn("Failed to log API call", e);
        }
    }

    private String getFullRequestPath(ContentCachingRequestWrapper request) {
        String queryString = request.getQueryString();
        return queryString != null ?
                request.getRequestURI() + "?" + queryString :
                request.getRequestURI();
    }

    private String getRequestBody(ContentCachingRequestWrapper request) throws UnsupportedEncodingException {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }

        String contentType = request.getContentType();
        String contentBody = new String(content, request.getCharacterEncoding());

        if (isJsonContent(contentType)) {
            return formatJson(contentBody);
        }
        return "Binary Content";
    }

    private String getResponseBody(ContentCachingResponseWrapper response) throws UnsupportedEncodingException {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return "";
        }

        String contentType = response.getContentType();
        String responseBody = new String(content, response.getCharacterEncoding());

        if (isJsonContent(contentType)) {
            return formatJson(responseBody);
        }
        return "Binary Content";
    }

    private boolean isJsonContent(String contentType) {
        if (contentType == null) return false;
        String lowerContentType = contentType.toLowerCase();
        return JSON_CONTENT_TYPES.stream()
                .anyMatch(lowerContentType::contains);
    }

    private String formatJson(String content) {
        try {
            Object json = objectMapper.readValue(content, Object.class);
            return objectMapper.writeValueAsString(json);
        } catch (Exception e) {
            log.warn("Failed to format JSON content", e);
            return content;
        }
    }
}
