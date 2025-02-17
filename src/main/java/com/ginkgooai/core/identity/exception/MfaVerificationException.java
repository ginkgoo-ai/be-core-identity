package com.ginkgooai.core.identity.exception;

import com.ginkgooai.core.common.exception.BaseRuntimeException;
import org.springframework.http.HttpStatus;

public class MfaVerificationException extends BaseRuntimeException {
   private static final String TYPE = "https://api.ginkgoo.com/errors/mfa-verification-failed";
   private static final String TITLE = "MFA Verification Failed";
   private static final HttpStatus STATUS = HttpStatus.BAD_REQUEST;

   public MfaVerificationException(String detail) {
       super(TYPE, TITLE, detail, STATUS);
   }
}