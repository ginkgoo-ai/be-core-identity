package com.benwk.ginkgoocoreidentity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Interface defining the contract for exceptions that can be converted to Problem Details.
 * Implements RFC 7807 Problem Details for HTTP APIs.
 */
public interface ProblemDetailAware {
    String getType();

    String getTitle();

    String getDetail();

    HttpStatus getStatus();

    default ProblemDetail toProblemDetail() {
        return ProblemDetailBuilder.forException(this);
    }
}
