package com.ginkgooai.core.identity.handler;

import com.ginkgooai.core.common.exception.BaseRuntimeException;
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

    @ExceptionHandler(BaseRuntimeException.class)
    public ProblemDetail handleBaseException(BaseRuntimeException ex) {
        log.error("BaseRuntimeException: ", ex);
        return ex.toProblemDetail();
    }
}
