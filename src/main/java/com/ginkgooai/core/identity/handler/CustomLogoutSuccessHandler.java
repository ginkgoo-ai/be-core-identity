package com.ginkgooai.core.identity.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcLogoutAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler implements AuthenticationSuccessHandler {

    private final JdbcOAuth2AuthorizationService authorizationService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        if (authentication instanceof OidcLogoutAuthenticationToken logoutToken) {
            try {
                OAuth2Authorization authorization = authorizationService
                        .findByToken(logoutToken.getIdToken().getTokenValue(), new OAuth2TokenType(OidcParameterNames.ID_TOKEN));

                if (authorization != null) {
                    authorizationService.remove(authorization);
                }

                String domain = request.getServerName();
                String[] cookiesToClear = {"JSESSIONID", "SESSION", "XSRF-TOKEN", "remember-me"};

                for (String cookieName : cookiesToClear) {
                    response.addHeader("Set-Cookie",
                            String.format("%s=; Path=/; Domain=%s; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; HttpOnly; SameSite=Lax",
                                    cookieName, domain));
                }

                HttpSession session = request.getSession(false);
                if (session != null) {
                    log.debug("Invalidating session: {}", session.getId());
                    session.invalidate();
                }
                SecurityContextHolder.clearContext();

                String postLogoutRedirectUri = logoutToken.getPostLogoutRedirectUri();
                if (!ObjectUtils.isEmpty(postLogoutRedirectUri)) {
                    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(postLogoutRedirectUri);
                    if (!ObjectUtils.isEmpty(logoutToken.getState())) {
                        builder.queryParam("state", logoutToken.getState());
                    }
                    log.debug("Redirecting to: {}", builder.build().toUriString());
                    response.sendRedirect(builder.build().toUriString());
                } else {
                    response.setStatus(HttpServletResponse.SC_OK);
                }
            } catch (Exception e) {
                log.error("Error during logout process", e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}