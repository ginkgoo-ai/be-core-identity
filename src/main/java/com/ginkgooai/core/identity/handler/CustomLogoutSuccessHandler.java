package com.ginkgooai.core.identity.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcLogoutAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Set;

@Component
@Slf4j
public class CustomLogoutSuccessHandler implements AuthenticationSuccessHandler {
   
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        if (authentication instanceof OidcLogoutAuthenticationToken logoutToken) {
            String subject = logoutToken.getIdToken().getSubject();
            
            try {
                String sessionPattern = "spring:session:sessions:" + subject + "*";
                Set<String> keys = redisTemplate.keys(sessionPattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.info("Cleared {} sessions for user {}", keys.size(), subject);
                }

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
}