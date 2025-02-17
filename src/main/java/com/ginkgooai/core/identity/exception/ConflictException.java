package com.ginkgooai.core.identity.exception;

import com.ginkgooai.core.common.exception.BaseRuntimeException;
import org.springframework.http.HttpStatus;

public class ConflictException extends BaseRuntimeException {
   private static final String TYPE = "https://api.ginkgoo.com/errors/resource-conflict";
   private static final String TITLE = "Resource Conflict";
   private static final HttpStatus STATUS = HttpStatus.CONFLICT;

   public ConflictException(String resourceName, String field, String value) {
       super(TYPE, TITLE, 
             String.format("%s with %s '%s' already exists", resourceName, field, value), 
             STATUS);
   }
}