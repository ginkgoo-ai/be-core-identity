package com.ginkgooai.core.identity.security;

import com.ginkgooai.core.identity.service.GuestCodeService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class GuestCodeGrantAuthenticationProvider implements AuthenticationProvider {

    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final GuestCodeService guestCodeService;

    public GuestCodeGrantAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
            GuestCodeService guestCodeService) {
        Assert.notNull(authorizationService, "authorizationService cannot be null");
        Assert.notNull(tokenGenerator, "tokenGenerator cannot be null");
        Assert.notNull(guestCodeService, "guestCodeService cannot be null");
        this.authorizationService = authorizationService;
        this.tokenGenerator = tokenGenerator;
        this.guestCodeService = guestCodeService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        GuestCodeGrantAuthenticationToken guestCodeAuthentication =
                (GuestCodeGrantAuthenticationToken) authentication;

        OAuth2ClientAuthenticationToken clientPrincipal =
                getAuthenticatedClientElseThrowInvalidClient(guestCodeAuthentication);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

        if (!registeredClient.getAuthorizationGrantTypes().contains(guestCodeAuthentication.getGrantType())) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }

        String guestCode = guestCodeAuthentication.getGuestCode();
        String resourceId = guestCodeAuthentication.getResourceId();
        GuestCodeService.GuestCodeInfo codeInfo = guestCodeService.validateGuestCode(guestCode, resourceId);

        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("resource_id", resourceId);
        additionalParameters.put("resource_owner", codeInfo.ownerEmail());
        additionalParameters.put("guest_email", codeInfo.guestEmail());
        additionalParameters.put("access_type", "guest");

        OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(clientPrincipal)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizationGrantType(guestCodeAuthentication.getGrantType())
                .authorizationGrant(guestCodeAuthentication)
                .put("resource_id", resourceId)
                .put("resource_owner", codeInfo.ownerEmail())
                .put("guest_email", codeInfo.guestEmail())
                .put("access_type", "guest")
                .build();

        OAuth2Token generatedAccessToken = this.tokenGenerator.generate(tokenContext);
        if (generatedAccessToken == null) {
            OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
                    "The token generator failed to generate the access token.", null);
            throw new OAuth2AuthenticationException(error);
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = codeInfo.expiresAt();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                generatedAccessToken.getTokenValue(), issuedAt, expiresAt,
                registeredClient.getScopes());

        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(codeInfo.guestEmail())
                .authorizationGrantType(guestCodeAuthentication.getGrantType())
                .attribute("resource_id", resourceId)
                .attribute("resource_owner", codeInfo.ownerEmail())
                .attribute("guest_email", codeInfo.guestEmail())
                .attribute("access_type", "guest");

        if (generatedAccessToken instanceof ClaimAccessor) {
            authorizationBuilder.token(accessToken, (metadata) ->
                    metadata.put(
                            OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
                            ((ClaimAccessor) generatedAccessToken).getClaims())
            );
        } else {
            authorizationBuilder.accessToken(accessToken);
        }

        OAuth2Authorization authorization = authorizationBuilder.build();

        this.authorizationService.save(authorization);

        return new OAuth2AccessTokenAuthenticationToken(
                registeredClient, clientPrincipal, accessToken, null, additionalParameters);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return GuestCodeGrantAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static OAuth2ClientAuthenticationToken getAuthenticatedClientElseThrowInvalidClient(Authentication authentication) {
        OAuth2ClientAuthenticationToken clientPrincipal = null;
        if (OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication.getPrincipal().getClass())) {
            clientPrincipal = (OAuth2ClientAuthenticationToken) authentication.getPrincipal();
        }
        if (clientPrincipal != null && clientPrincipal.isAuthenticated()) {
            return clientPrincipal;
        }
        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
    }
}
