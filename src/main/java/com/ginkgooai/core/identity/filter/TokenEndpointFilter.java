package com.ginkgooai.core.identity.filter;

import com.ginkgooai.core.identity.domain.OAuth2RegisteredClient;
import com.ginkgooai.core.identity.repository.OAuth2RegisteredClientRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TokenEndpointFilter extends OncePerRequestFilter {

    private final OAuth2RegisteredClientRepository clientRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        if (!request.getRequestURI().equals("/oauth2/token")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = request.getParameter("client_id");
        if (clientId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing client_id parameter");
            return;
        }

        OAuth2RegisteredClient client = clientRepository.findByClientId(clientId).get();
        if (client == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid client_id");
            return;
        }

        HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
            private final Map<String, String[]> modifiedParameters = new HashMap<>(
                    super.getParameterMap()
            );

            {
                modifiedParameters.put("client_secret", 
                    new String[]{client.getClientSecretRaw()});
            }

            @Override
            public String getParameter(String name) {
                String[] values = modifiedParameters.get(name);
                return values != null && values.length > 0 ? values[0] : null;
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return Collections.unmodifiableMap(modifiedParameters);
            }

            @Override
            public Enumeration<String> getParameterNames() {
                return Collections.enumeration(modifiedParameters.keySet());
            }

            @Override
            public String[] getParameterValues(String name) {
                return modifiedParameters.get(name);
            }
        };

        filterChain.doFilter(wrappedRequest, response);
    }
}
