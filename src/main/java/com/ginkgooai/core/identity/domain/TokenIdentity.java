package com.ginkgooai.core.identity.domain;

import com.ginkgooai.core.identity.exception.InvalidVerificationCodeException;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@AllArgsConstructor
@Data
public class TokenIdentity {
    String clientId;
    
    String userId;
    
    @Override
    public String toString() {
        return clientId + ":" + userId;
    }
    
    public TokenIdentity(String token) throws InvalidVerificationCodeException {
        String tokenData = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        String[] parts = tokenData.split(":", 3);
        if (parts.length != 3) {
            throw new InvalidVerificationCodeException("Invalid token format");
        }

        this.clientId = parts[0];
        this.userId = parts[1];
    }
}
