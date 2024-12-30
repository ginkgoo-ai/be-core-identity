package com.ginkgooai.core.identity.exception;

import org.springframework.http.HttpStatus;

public class TokenRequestTooFrequentException extends BaseRuntimeException {
    private static final String TYPE = "https://api.ginkgoo.com/errors/token-request-too-frequent";
    private static final String TITLE = "Token Request Too Frequent";
    private static final HttpStatus STATUS = HttpStatus.TOO_MANY_REQUESTS;

    public TokenRequestTooFrequentException(String detail) {
        super(TYPE, TITLE, detail, STATUS);
    }
}