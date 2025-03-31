package com.ginkgooai.core.identity.security;

import com.ginkgooai.core.common.security.CustomGrantTypes;
import com.ginkgooai.core.identity.domain.UserInfo;
import com.ginkgooai.core.identity.service.ShareCodeService;
import com.ginkgooai.core.identity.service.UserService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.*;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ShareCodeGrantAuthenticationProvider implements AuthenticationProvider {

    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final ShareCodeService shareCodeService;
    private final UserService userService;

    public ShareCodeGrantAuthenticationProvider(
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
            ShareCodeService shareCodeService,
            UserService userService) {
        Assert.notNull(authorizationService, "authorizationService cannot be null");
        Assert.notNull(tokenGenerator, "tokenGenerator cannot be null");
        Assert.notNull(shareCodeService, "shareCodeService cannot be null");
        this.authorizationService = authorizationService;
        this.tokenGenerator = tokenGenerator;
        this.shareCodeService = shareCodeService;
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ShareCodeGrantAuthenticationToken shareCodeAuthentication = (ShareCodeGrantAuthenticationToken) authentication;

        OAuth2ClientAuthenticationToken clientPrincipal = getAuthenticatedClientElseThrowInvalidClient(
            shareCodeAuthentication);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

        if (!registeredClient.getAuthorizationGrantTypes().contains(CustomGrantTypes.SHARE_CODE)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }

        String shareCode = shareCodeAuthentication.getShareCode();
        ShareCodeService.ShareCodeInfo codeInfo = shareCodeService.validateShareCode(shareCode);

        Set<String> scopes = new HashSet<>();
        scopes.add(String.join(":", codeInfo.resource(), codeInfo.resourceId(), "read"));
        if (codeInfo.write()) {
            scopes.add(String.join(":", codeInfo.resource(), codeInfo.resourceId(), "write"));
        }

        UserInfo user = userService.getUserById(codeInfo.userId());

        ShareCodeGrantAuthenticationToken grantAuthenticationToken = ShareCodeGrantAuthenticationToken
            .withAuthorities(shareCodeAuthentication, user.getAuthorities());
        grantAuthenticationToken.setUserName(user.getName());
        grantAuthenticationToken.setEmail(user.getEmail());
        grantAuthenticationToken.setWorkspaceId(codeInfo.workspaceId());
        OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(grantAuthenticationToken)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
            .authorizationGrantType(CustomGrantTypes.SHARE_CODE)
            .authorizationGrant(shareCodeAuthentication)
                .authorizedScopes(scopes)
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

        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName(user.getName())
            .authorizationGrantType(CustomGrantTypes.SHARE_CODE)
            .attribute("resource_id", codeInfo.resourceId())
            .attribute("sub", user.getId())
            .attribute("email", user.getEmail())
            .attribute("name", user.getName())
                .attribute("access_type", "guest")
                .attribute("workspace_id", codeInfo.workspaceId())
                .token(accessToken, (metadata) -> {
                    if (generatedAccessToken instanceof ClaimAccessor) {
                        metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
                                ((ClaimAccessor) generatedAccessToken).getClaims());
                    }
                })
                .build();

        this.authorizationService.save(authorization);

        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put("resource_id", codeInfo.resourceId());
        additionalParameters.put("sub", user.getId());
        additionalParameters.put("email", user.getEmail());
        additionalParameters.put("name", user.getName());
        additionalParameters.put("access_type", "guest");
        additionalParameters.put("workspace_id", codeInfo.workspaceId());

        return new OAuth2AccessTokenAuthenticationToken(
                registeredClient, grantAuthenticationToken, accessToken, null, additionalParameters);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ShareCodeGrantAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static OAuth2ClientAuthenticationToken getAuthenticatedClientElseThrowInvalidClient(
            Authentication authentication) {
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
