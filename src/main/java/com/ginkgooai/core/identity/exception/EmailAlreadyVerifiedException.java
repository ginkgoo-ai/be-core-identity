package com.ginkgooai.core.identity.exception;

import com.ginkgooai.core.common.exception.BaseRuntimeException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyVerifiedException extends BaseRuntimeException {
    
    private static final String TYPE = "https://api.ginkgoo.com/errors/email-already-verified";
    private static final String TITLE = "Email Already Verified";
    private static final HttpStatus STATUS = HttpStatus.CONFLICT;

    public EmailAlreadyVerifiedException(String detail) {
        super(TYPE, TITLE, detail, STATUS);
    }

    // Overload constructor with default detail message
    public EmailAlreadyVerifiedException() {
        this("The email address has already been verified");
    }
}