package com.ginkgooai.core.identity.handler;

import com.ginkgooai.core.identity.exception.BaseRuntimeException;
import com.ginkgooai.core.identity.exception.UnexpectedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ProblemDetail handleOAuth2AuthenticationException(OAuth2AuthenticationException ex) {
        log.error("OAuth2 authentication failed", ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage()
        );
        problemDetail.setTitle("OAuth2 Authentication Failed");
        problemDetail.setProperty("error_code", "OAUTH2_ERROR");
        problemDetail.setProperty("timestamp", System.currentTimeMillis());
        return problemDetail;
    }

    @ExceptionHandler(BaseRuntimeException.class)
    public ProblemDetail handleBaseException(BaseRuntimeException ex) {
        log.error("BaseRuntimeException: ", ex);
        return ex.toProblemDetail();
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGlobalException(Exception ex) {
        log.error("Exception: ", ex);
        return new UnexpectedException(ex).toProblemDetail();
    }
}
