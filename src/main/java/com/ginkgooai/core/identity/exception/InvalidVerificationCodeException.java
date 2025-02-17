package com.ginkgooai.core.identity.exception;

import com.ginkgooai.core.common.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidVerificationCodeException extends BaseException {
    private static final String TYPE = "https://api.ginkgoo.com/errors/invalid-verification-token";
    private static final String TITLE = "Invalid Verification Token";
    private static final HttpStatus STATUS = HttpStatus.BAD_REQUEST;

    public InvalidVerificationCodeException(String detail) {
        super(TYPE, TITLE, detail, STATUS);
    }
}