package com.ginkgooai.core.identity.handler;

import com.ginkgooai.core.common.exception.BaseRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.view.RedirectView;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BaseRuntimeException.class)
    public ProblemDetail handleBaseException(BaseRuntimeException ex) {
        log.error("BaseRuntimeException: ", ex);
        return ex.toProblemDetail();
	}

	@ExceptionHandler(OAuth2AuthenticationException.class)
	public Object handleOAuth2AuthenticationException(OAuth2AuthenticationException ex, HttpServletRequest request) {
		log.warn("OAuth2AuthenticationException: {}", ex.getMessage());

		// Check if this is a web request that should redirect to login page
		String acceptHeader = request.getHeader("Accept");
		if (acceptHeader != null && acceptHeader.contains("text/html")) {
			// Store the exception message in session for display on login page
			request.getSession().setAttribute("SPRING_SECURITY_LAST_EXCEPTION", ex);
			return new RedirectView("/login?error=true");
		}

		// For API requests, return a problem detail
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
		problemDetail.setProperty("error", ex.getError().getErrorCode());
		return problemDetail;
    }
}
