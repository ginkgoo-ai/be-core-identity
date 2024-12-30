package com.ginkgooai.core.identity.exception;

import org.springframework.http.HttpStatus;

public class MfaLockedException extends BaseRuntimeException {
   private static final String TYPE = "https://api.ginkgoo.com/errors/mfa-locked";
   private static final String TITLE = "MFA Locked";
   private static final HttpStatus STATUS = HttpStatus.FORBIDDEN;

   public MfaLockedException(String detail) {
       super(TYPE, TITLE, detail, STATUS);
   }
}
