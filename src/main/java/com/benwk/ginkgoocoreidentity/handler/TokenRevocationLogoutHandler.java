package com.benwk.ginkgoocoreidentity.handler;

import com.benwk.ginkgoocoreidentity.service.CachedOAuth2AuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenRevocationLogoutHandler implements LogoutHandler {

    private final CachedOAuth2AuthorizationService authorizationService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response,
                       Authentication authentication) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            OAuth2Authorization authorization = authorizationService
                .findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
            
            if (authorization != null) {
                authorizationService.remove(authorization);
            }
        }
    }
}