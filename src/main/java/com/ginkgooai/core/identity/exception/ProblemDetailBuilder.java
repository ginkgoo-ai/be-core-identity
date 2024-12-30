package com.ginkgooai.core.identity.exception;

import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;

/**
 * Utility class for building RFC 7807 Problem Detail responses
 */
public class ProblemDetailBuilder {
    private static final String BASE_TYPE_URL = "https://api.ginkgoocoreidentity.com/errors/";

    /**
     * Creates a ProblemDetail from a ProblemDetailAware exception
     */
    public static ProblemDetail forException(ProblemDetailAware ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                ex.getStatus(),
                ex.getDetail()
        );

        problemDetail.setTitle(ex.getTitle());
        problemDetail.setType(URI.create(BASE_TYPE_URL + ex.getType()));
        problemDetail.setProperty("timestamp", Instant.now());

        // Add specific properties for ResourceNotFoundException
        if (ex instanceof ResourceNotFoundException rnf) {
            problemDetail.setProperty("resourceName", rnf.getResourceName());
            problemDetail.setProperty("fieldName", rnf.getFieldName());
            problemDetail.setProperty("fieldValue", rnf.getFieldValue());
        }

        return problemDetail;
    }
}
