package com.ginkgooai.core.identity.security;

import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GuestCodeGrantAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;
    private static final AuthorizationGrantType GUEST_CODE = 
            new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:guest_code");

    private final String guestCode;
    private final String resourceId;
    private final Authentication clientPrincipal;
    private final Map<String, Object> additionalParameters;

    public GuestCodeGrantAuthenticationToken(String guestCode, 
                                          String resourceId,
                                          Authentication clientPrincipal,
                                          @Nullable Map<String, Object> additionalParameters) {
        super(Collections.emptyList());
        Assert.hasText(guestCode, "guestCode cannot be empty");
        Assert.hasText(resourceId, "resourceId cannot be empty");
        Assert.notNull(clientPrincipal, "clientPrincipal cannot be null");
        this.guestCode = guestCode;
        this.resourceId = resourceId;
        this.clientPrincipal = clientPrincipal;
        this.additionalParameters = additionalParameters != null ?
                Collections.unmodifiableMap(new HashMap<>(additionalParameters)) :
                Collections.emptyMap();
        setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return this.clientPrincipal;
    }

    public String getGuestCode() {
        return this.guestCode;
    }
    
    public String getResourceId() {
        return this.resourceId;
    }

    public AuthorizationGrantType getGrantType() {
        return GUEST_CODE;
    }

    public Map<String, Object> getAdditionalParameters() {
        return this.additionalParameters;
    }
}
