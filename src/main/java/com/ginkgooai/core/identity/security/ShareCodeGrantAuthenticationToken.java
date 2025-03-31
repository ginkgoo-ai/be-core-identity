package com.ginkgooai.core.identity.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ShareCodeGrantAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;
    private String name;
    private String userName;
    private String email;
    private String workspaceId;
    private final String shareCode;
    private final Authentication clientPrincipal;
    private final Map<String, Object> additionalParameters;

    /**
     * Constructor with authorities
     */
    public ShareCodeGrantAuthenticationToken(String shareCode,
                                             Authentication clientPrincipal,
                                             @Nullable Map<String, Object> additionalParameters,
                                             Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        Assert.hasText(shareCode, "shareCode cannot be empty");
        Assert.notNull(clientPrincipal, "clientPrincipal cannot be null");
        this.name = shareCode;
        this.shareCode = shareCode;
        this.clientPrincipal = clientPrincipal;
        this.additionalParameters = additionalParameters != null ?
                Collections.unmodifiableMap(new HashMap<>(additionalParameters)) :
                Collections.emptyMap();
        setAuthenticated(false);
    }

    /**
     * Backward compatible constructor without authorities
     */
    public ShareCodeGrantAuthenticationToken(String shareCode,
                                             Authentication clientPrincipal,
                                             @Nullable Map<String, Object> additionalParameters) {
        this(shareCode, clientPrincipal, additionalParameters, Collections.emptyList());
    }

    /**
     * Factory method to create a token with authorities
     */
    public static ShareCodeGrantAuthenticationToken withAuthorities(
        ShareCodeGrantAuthenticationToken token,
            Collection<? extends GrantedAuthority> authorities) {
        return new ShareCodeGrantAuthenticationToken(
            token.getShareCode(),
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

    public Map<String, Object> extractClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("share_code", this.shareCode);
        claims.put("client_id", this.clientPrincipal.getName());
        claims.put("workspace_id", this.workspaceId);
        claims.put("email", this.email);
        claims.put("name", this.name);
        return claims;
    }
        
}