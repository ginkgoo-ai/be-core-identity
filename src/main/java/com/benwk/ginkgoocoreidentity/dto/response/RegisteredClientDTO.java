package com.benwk.ginkgoocoreidentity.dto.response;

import com.benwk.ginkgoocoreidentity.dto.ClientSettingsDTO;
import com.benwk.ginkgoocoreidentity.dto.TokenSettingsDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class RegisteredClientDTO {
    private String id;
    private String clientId;
    private String clientName;
    private Set<String> authenticationMethods;
    private Set<String> grantTypes;
    private Set<String> redirectUris;
    private Set<String> scopes;
    private ClientSettingsDTO clientSettings;
    private TokenSettingsDTO tokenSettings;
    
    public static RegisteredClientDTO from(RegisteredClient client) {
        RegisteredClientDTO dto = new RegisteredClientDTO();
        dto.setId(client.getId());
        dto.setClientId(client.getClientId());
        dto.setClientName(client.getClientName());
        dto.setAuthenticationMethods(client.getClientAuthenticationMethods().stream()
            .map(ClientAuthenticationMethod::getValue)
            .collect(Collectors.toSet()));
        dto.setGrantTypes(client.getAuthorizationGrantTypes().stream()
            .map(AuthorizationGrantType::getValue)
            .collect(Collectors.toSet()));
        dto.setRedirectUris(new HashSet<>(client.getRedirectUris()));
        dto.setScopes(new HashSet<>(client.getScopes()));
        
        // 转换客户端设置
        ClientSettingsDTO settingsDTO = new ClientSettingsDTO();
        settingsDTO.setRequireAuthorizationConsent(
            client.getClientSettings().isRequireAuthorizationConsent());
        settingsDTO.setRequireProofKey(
            client.getClientSettings().isRequireProofKey());
        dto.setClientSettings(settingsDTO);
        
        // 转换令牌设置
        TokenSettingsDTO tokenDTO = new TokenSettingsDTO();
        tokenDTO.setAccessTokenTimeToLive(
            client.getTokenSettings().getAccessTokenTimeToLive().getSeconds());
        tokenDTO.setRefreshTokenTimeToLive(
            client.getTokenSettings().getRefreshTokenTimeToLive().getSeconds());
        tokenDTO.setReuseRefreshTokens(
            client.getTokenSettings().isReuseRefreshTokens());
        dto.setTokenSettings(tokenDTO);
        
        return dto;
    }

}