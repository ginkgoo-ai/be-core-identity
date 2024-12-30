package com.benwk.ginkgoocoreidentity.exception;

import org.springframework.http.HttpStatus;

public class InvalidPasswordException extends BaseRuntimeException {
    private static final String TYPE = "https://api.ginkgoo.com/errors/invalid-password";
    private static final String TITLE = "Invalid Password";
    private static final HttpStatus STATUS = HttpStatus.BAD_REQUEST;

    public InvalidPasswordException(String detail) {
        super(TYPE, TITLE, detail, STATUS);
    }
}