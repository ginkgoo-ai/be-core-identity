package com.ginkgooai.core.identity.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends BaseRuntimeException {
    private static final String TYPE = "https://api.ginkgoo.com/errors/email-already-exists";
    private static final String TITLE = "Email Already Exists";
    private static final HttpStatus STATUS = HttpStatus.CONFLICT;

    public EmailAlreadyExistsException(String detail) {
        super(TYPE, TITLE, detail, STATUS);
    }
}