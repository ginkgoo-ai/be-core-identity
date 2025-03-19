package com.ginkgooai.core.identity.security;

import com.ginkgooai.core.common.security.CustomGrantTypes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GuestCodeGrantAuthenticationConverter implements AuthenticationConverter {

    private static final String GRANT_TYPE_GUEST_CODE = CustomGrantTypes.GUEST_CODE.getValue(); 
    private static final String GUEST_CODE_PARAMETER = "guest_code";
    private static final String RESOURCE_ID_PARAMETER = "resource_id";

    @Nullable
    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!GRANT_TYPE_GUEST_CODE.equals(grantType)) {
            return null;
        }

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        MultiValueMap<String, String> parameters = getParameters(request);

        String guestCode = parameters.getFirst(GUEST_CODE_PARAMETER);
        if (!StringUtils.hasText(guestCode) || parameters.get(GUEST_CODE_PARAMETER).size() != 1) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        String resourceId = parameters.getFirst(RESOURCE_ID_PARAMETER);
        if (!StringUtils.hasText(resourceId) || parameters.get(RESOURCE_ID_PARAMETER).size() != 1) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        Map<String, Object> additionalParameters = new HashMap<>();
        parameters.forEach((key, value) -> {
            if (!key.equals(OAuth2ParameterNames.GRANT_TYPE) &&
                !key.equals(OAuth2ParameterNames.CLIENT_ID) &&
                !key.equals(GUEST_CODE_PARAMETER) &&
                !key.equals(RESOURCE_ID_PARAMETER)) {
                additionalParameters.put(key, value.get(0));
            }
        });

        return new GuestCodeGrantAuthenticationToken(
                guestCode, 
                resourceId,
                clientPrincipal, 
                additionalParameters
        );
    }
    
    //set authorities
    public void setAuthorities() {
        
    }

    private static MultiValueMap<String, String> getParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>(parameterMap.size());
        parameterMap.forEach((key, values) -> {
            if (values.length > 0) {
                for (String value : values) {
                    parameters.add(key, value);
                }
            }
        });
        return parameters;
    }
}
