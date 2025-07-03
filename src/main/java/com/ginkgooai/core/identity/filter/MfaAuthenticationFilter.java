package com.ginkgooai.core.identity.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ginkgooai.core.identity.service.MfaAuthenticationService;
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
				handleMfaRequired(request, response, result);
                return;
            }

            chain.doFilter(request, response);

        } catch (Exception e) {
			handleAuthenticationError(request, response, e);
		}
	}

	private void handleMfaRequired(HttpServletRequest request, HttpServletResponse response, Map<String, Object> result)
			throws IOException {
		if (isWebFormRequest(request)) {
			// For web form requests, redirect to login page with MFA error
			response.sendRedirect("/login?error=mfa_required");
		}
		else {
			// For API requests, return JSON response
			writeJsonResponse(response, HttpStatus.UNAUTHORIZED, result);
        }
    }

    private void writeJsonResponse(HttpServletResponse response,
                                   HttpStatus status,
                                   Map<String, Object> body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

	private void handleAuthenticationError(HttpServletRequest request, HttpServletResponse response, Exception e)
			throws IOException {
		if (isWebFormRequest(request)) {
			// For web form requests, redirect to login page with error
			// Store the exception in session for display on login page
			request.getSession().setAttribute("SPRING_SECURITY_LAST_EXCEPTION", e);
			response.sendRedirect("/login?error=true");
		}
		else {
			// For API requests, return JSON response
			Map<String, Object> error = Map.of("error", e.getMessage());
			writeJsonResponse(response, HttpStatus.UNAUTHORIZED, error);
		}
	}

	private boolean isWebFormRequest(HttpServletRequest request) {
		String acceptHeader = request.getHeader("Accept");
		String contentType = request.getContentType();

		// Check if this is a web form request (HTML accept header or form content type)
		return (acceptHeader != null && acceptHeader.contains("text/html"))
				|| (contentType != null && contentType.contains("application/x-www-form-urlencoded"));
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return request.getMethod().equals("POST") && request.getRequestURI().equals("/login");
    }
}