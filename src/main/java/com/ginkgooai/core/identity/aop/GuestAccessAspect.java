package com.ginkgooai.core.identity.aop;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class GuestAccessAspect {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    @Around("@annotation(guestAccessDenied)")
    public Object handleGuestAccess(ProceedingJoinPoint joinPoint, GuestAccessDenied guestAccessDenied) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (isGuest(authentication)) {
            clearSecurityContext();
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new RuntimeException(guestAccessDenied.message()));
        }

        return joinPoint.proceed();
    }

    private boolean isGuest(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .noneMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }

    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        SecurityContextLogoutHandler securityContextLogoutHandler = new SecurityContextLogoutHandler();
        securityContextLogoutHandler.logout(request, response, null);
    }
}