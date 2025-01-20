package com.ginkgooai.core.identity.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    
    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) 
            throws IOException, ServletException {
        
        log.error("OAuth2 Authentication failed", exception);
        
        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) exception;
            log.error("OAuth2 Error Code: {}", oauth2Exception.getError().getErrorCode());
            log.error("OAuth2 Error Description: {}", oauth2Exception.getError().getDescription());
        }
        
        super.onAuthenticationFailure(request, response, exception);
    }
}
