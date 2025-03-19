package com.ginkgooai.core.identity.security;

import com.ginkgooai.core.common.security.CustomGrantTypes;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class GuestCodeGrantAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;
    private String name;
    private String userName;
    private String email;
    private final String guestCode;
    private final String resourceId;
    private final Authentication clientPrincipal;
    private final Map<String, Object> additionalParameters;

    /**
     * Constructor with authorities
     */
    public GuestCodeGrantAuthenticationToken(String guestCode,
                                             String resourceId,
                                             Authentication clientPrincipal,
                                             @Nullable Map<String, Object> additionalParameters,
                                             Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        Assert.hasText(guestCode, "guestCode cannot be empty");
        Assert.hasText(resourceId, "resourceId cannot be empty");
        Assert.notNull(clientPrincipal, "clientPrincipal cannot be null");
        this.name = guestCode;
        this.guestCode = guestCode;
        this.resourceId = resourceId;
        this.clientPrincipal = clientPrincipal;
        this.additionalParameters = additionalParameters != null ?
                Collections.unmodifiableMap(new HashMap<>(additionalParameters)) :
                Collections.emptyMap();
        setAuthenticated(false);
    }

    /**
     * Backward compatible constructor without authorities
     */
    public GuestCodeGrantAuthenticationToken(String guestCode,
                                             String resourceId,
                                             Authentication clientPrincipal,
                                             @Nullable Map<String, Object> additionalParameters) {
        this(guestCode, resourceId, clientPrincipal, additionalParameters, Collections.emptyList());
    }

    /**
     * Factory method to create a token with authorities
     */
    public static GuestCodeGrantAuthenticationToken withAuthorities(
            GuestCodeGrantAuthenticationToken token,
            Collection<? extends GrantedAuthority> authorities) {
        return new GuestCodeGrantAuthenticationToken(
                token.getGuestCode(),
                token.getResourceId(),
                token.getClientPrincipal(),
                token.getAdditionalParameters(),
                authorities
        );
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return this.clientPrincipal;
    }
    
}