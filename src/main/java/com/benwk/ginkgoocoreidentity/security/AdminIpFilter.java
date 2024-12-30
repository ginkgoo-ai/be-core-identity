package com.benwk.ginkgoocoreidentity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AdminIpFilter extends OncePerRequestFilter {

    private final AdminSecurityProperties properties;
    private final List<IpAddressMatcher> ipMatchers;

    public AdminIpFilter(AdminSecurityProperties properties) {
        this.properties = properties;
        this.ipMatchers = properties.getIpWhitelist().stream()
                .map(IpAddressMatcher::new)
                .collect(Collectors.toList());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

        String remoteAddr = request.getRemoteAddr();
        boolean allowed = ipMatchers.stream().anyMatch(matcher -> matcher.matches(remoteAddr));

        if (!allowed) {
            response.sendError(HttpStatus.FORBIDDEN.value(), 
                    "Access denied: IP address not in whitelist");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return !path.startsWith("/api/v1/oauth2/");
    }
}