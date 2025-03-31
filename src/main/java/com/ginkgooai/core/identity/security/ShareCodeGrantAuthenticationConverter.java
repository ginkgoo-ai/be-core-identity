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

import java.util.HashMap;
import java.util.Map;

public class ShareCodeGrantAuthenticationConverter implements AuthenticationConverter {

    private static final String GRANT_TYPE = CustomGrantTypes.SHARE_CODE.getValue();
    private static final String SHARE_CODE_PARAMETER = "share_code";
    private static final String RESOURCE_ID_PARAMETER = "resource_id";

    @Nullable
    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!GRANT_TYPE.equals(grantType)) {
            return null;
        }

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        MultiValueMap<String, String> parameters = getParameters(request);

        String shareCode = parameters.getFirst(SHARE_CODE_PARAMETER);
        if (!StringUtils.hasText(shareCode) || parameters.get(SHARE_CODE_PARAMETER).size() != 1) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        Map<String, Object> additionalParameters = new HashMap<>();
        parameters.forEach((key, value) -> {
            if (!key.equals(OAuth2ParameterNames.GRANT_TYPE) &&
                !key.equals(OAuth2ParameterNames.CLIENT_ID) &&
                !key.equals(SHARE_CODE_PARAMETER) &&
                !key.equals(RESOURCE_ID_PARAMETER)) {
                additionalParameters.put(key, value.get(0));
            }
        });

        return new ShareCodeGrantAuthenticationToken(
            shareCode, 
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
