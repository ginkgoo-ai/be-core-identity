package com.ginkgooai.core.identity.exception;

import com.ginkgooai.core.common.exception.BaseRuntimeException;
import org.springframework.http.HttpStatus;

public class EmailSendException extends BaseRuntimeException {
    private static final String TYPE = "https://api.ginkgoo.com/errors/email-send-failed";
    private static final String TITLE = "Email Send Failed";
    private static final HttpStatus STATUS = HttpStatus.INTERNAL_SERVER_ERROR;

    public EmailSendException(String detail) {
        super(TYPE, TITLE, detail, STATUS);
    }
}