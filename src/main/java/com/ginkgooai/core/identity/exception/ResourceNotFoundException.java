package com.ginkgooai.core.identity.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource cannot be found.
 * Provides specific details about the resource that was not found.
 */
@Getter
public class ResourceNotFoundException extends BaseRuntimeException {
    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(
            "not-found",
            "Resource Not Found",
            String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue),
            HttpStatus.NOT_FOUND
        );
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
}
