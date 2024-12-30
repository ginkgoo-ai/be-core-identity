package com.ginkgooai.core.identity.exception;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Constants class for error-related values.
 * Contains base URLs and utility methods for error handling.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorConstants {
    public static final String BASE_TYPE_URL = "https://api.ginkgoocoreidentity.com/errors/";
    
    public static String buildTypeUrl(String type) {
        return BASE_TYPE_URL + type;
    }
}
