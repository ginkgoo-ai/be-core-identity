package com.ginkgooai.core.identity.exception;

import com.ginkgooai.core.common.exception.BaseRuntimeException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class ValidationException extends BaseRuntimeException {
    private final Map<String, String> violations;

    public ValidationException(Map<String, String> violations) {
        super(
            "validation-error",
            "Validation Failed",
            "One or more fields failed validation",
            HttpStatus.BAD_REQUEST
        );
        this.violations = violations;
    }
}
