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
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public class ProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {
    @Value("${AUTH_SERVER}")
    private String authServer;

	private final LoginUrlAuthenticationEntryPoint loginUrlAuthenticationEntryPoint = new LoginUrlAuthenticationEntryPoint(
			"/login");

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
		if (isWebRequest(request)) {
			// For web requests, redirect to login page
			try {
				loginUrlAuthenticationEntryPoint.commence(request, response, authException);
			}
			catch (Exception e) {
				// Fallback to redirect
				response.sendRedirect("/login");
			}
		}
		else {
			// For API requests, return JSON Problem Details
			handleApiException(request, response, authException, HttpStatus.UNAUTHORIZED, "unauthorized");
		}
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
		if (isWebRequest(request)) {
			// For web requests, redirect to login page with access denied error
			response.sendRedirect("/login?error=access_denied");
		}
		else {
			// For API requests, return JSON Problem Details
			handleApiException(request, response, accessDeniedException, HttpStatus.FORBIDDEN, "forbidden");
		}
	}

	private boolean isWebRequest(HttpServletRequest request) {
		String acceptHeader = request.getHeader("Accept");
		String contentType = request.getContentType();

		// Check if this is a web request (HTML accept header or form content type)
		return (acceptHeader != null && acceptHeader.contains("text/html"))
				|| (contentType != null && contentType.contains("application/x-www-form-urlencoded")) ||
				// Also check for common web browser user agents as fallback
				isWebBrowserRequest(request);
    }

	private boolean isWebBrowserRequest(HttpServletRequest request) {
		String userAgent = request.getHeader("User-Agent");
		return userAgent != null && (userAgent.contains("Mozilla") || userAgent.contains("Chrome")
				|| userAgent.contains("Safari") || userAgent.contains("Edge"));
	}

	private void handleApiException(HttpServletRequest request, HttpServletResponse response,
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