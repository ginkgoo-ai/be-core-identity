package com.ginkgooai.core.identity.exception;

import com.ginkgooai.core.common.exception.BaseRuntimeException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UnexpectedException extends BaseRuntimeException {
    private final String errorClass;

    public UnexpectedException(Exception ex) {
        super(
            "internal-error",
            "Internal Server Error",
            "An unexpected error occurred",
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        this.errorClass = ex.getClass().getName();
    }
}

