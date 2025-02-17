package com.ginkgooai.core.identity.exception;

import com.ginkgooai.core.common.exception.BaseRuntimeException;
import org.springframework.http.HttpStatus;

public class RoleNotFoundException extends BaseRuntimeException {
    private static final String TYPE = "https://api.ginkgoo.com/errors/role-not-found";
    private static final String TITLE = "Role Not Found";
    private static final HttpStatus STATUS = HttpStatus.INTERNAL_SERVER_ERROR;

    public RoleNotFoundException(String detail) {
        super(TYPE, TITLE, detail, STATUS);
    }
}