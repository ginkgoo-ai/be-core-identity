package com.ginkgooai.core.identity.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        if (authentication instanceof OidcLogoutAuthenticationToken logoutToken) {
            try {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }

                SecurityContextHolder.clearContext();

                clearCookies(request, response);

                String postLogoutRedirectUri = logoutToken.getPostLogoutRedirectUri();
                if (!ObjectUtils.isEmpty(postLogoutRedirectUri)) {
                    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(postLogoutRedirectUri);
                    if (!ObjectUtils.isEmpty(logoutToken.getState())) {
                        builder.queryParam("state", logoutToken.getState());
                    }
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

    private void clearCookies(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().startsWith("JSESSIONID") ||
                        cookie.getName().startsWith("SESSION")) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }
}