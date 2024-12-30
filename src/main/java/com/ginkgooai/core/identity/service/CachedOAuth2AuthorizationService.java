package com.ginkgooai.core.identity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@CacheConfig(cacheNames = "oauth2_authorization")
public class CachedOAuth2AuthorizationService extends JdbcOAuth2AuthorizationService {

    private final JdbcTemplate jdbcTemplate;
    private final RegisteredClientRepository registeredClientRepository;
    private final CacheManager cacheManager;

    public CachedOAuth2AuthorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository,
            CacheManager cacheManager) {
        super(jdbcTemplate, registeredClientRepository);
        this.jdbcTemplate = jdbcTemplate;
        this.registeredClientRepository = registeredClientRepository;
        this.cacheManager = cacheManager;
    }

    @Override
//    @Cacheable(key = "#id", unless = "#result == null")
    public OAuth2Authorization findById(String id) {
        return super.findById(id);
    }

    @Override
//    @Cacheable(key = "'token:' + #token + ':' + #tokenType", unless = "#result == null")
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        return super.findByToken(token, tokenType);
    }

    /**
     * Find all valid (non-expired) authorizations with pagination
     *
     * @param pageable pagination information
     * @return Page of OAuth2Authorization
     */
    public Page<OAuth2Authorization> findAllValid(Pageable pageable) {
        // Get total count of valid authorizations
        String countSql = """
            SELECT COUNT(*)
            FROM oauth2_authorization
            WHERE access_token_expires_at > ?
            """;

        int total = jdbcTemplate.queryForObject(
                countSql,
                Integer.class,
                Instant.now()
        );

        // Query valid authorizations with pagination
        String sql = """
            SELECT id, registered_client_id, principal_name, authorization_grant_type,
                   authorized_scopes, attributes, state, authorization_code_value,
                   authorization_code_issued_at, authorization_code_expires_at,
                   authorization_code_metadata, access_token_value,
                   access_token_issued_at, access_token_expires_at,
                   access_token_metadata, access_token_type,
                   access_token_scopes, refresh_token_value,
                   refresh_token_issued_at, refresh_token_expires_at,
                   refresh_token_metadata
            FROM oauth2_authorization
            WHERE access_token_expires_at > ?
            ORDER BY access_token_issued_at DESC
            LIMIT ? OFFSET ?
            """;

        List<OAuth2Authorization> authorizations = jdbcTemplate.query(
                sql,
                this::extractAuthorization,
                Instant.now(),
                pageable.getPageSize(),
                pageable.getOffset()
        );

        return new PageImpl<>(
                authorizations,
                pageable,
                total
        );
    }

    /**
     * Extract OAuth2Authorization from ResultSet
     */
    private OAuth2Authorization extractAuthorization(ResultSet rs, int rowNum)
            throws SQLException {
        RegisteredClient registeredClient = registeredClientRepository.findById(
                rs.getString("registered_client_id")
        );

        OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id(rs.getString("id"))
                .principalName(rs.getString("principal_name"))
                .authorizationGrantType(resolveAuthorizationGrantType(rs.getString("authorization_grant_type")))
                .authorizedScopes(StringUtils.commaDelimitedListToSet(rs.getString("authorized_scopes")));

        // Handle access token
        String accessTokenValue = rs.getString("access_token_value");
        if (accessTokenValue != null) {
            OAuth2AccessToken accessToken = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    accessTokenValue,
                    rs.getTimestamp("access_token_issued_at").toInstant(),
                    rs.getTimestamp("access_token_expires_at").toInstant(),
                    StringUtils.commaDelimitedListToSet(rs.getString("access_token_scopes"))
            );
            builder.token(accessToken, metadata -> {
                try {
                    parseTokenMetadata(
                            rs.getString("access_token_metadata"),
                            metadata
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // Handle refresh token if exists
        String refreshTokenValue = rs.getString("refresh_token_value");
        if (refreshTokenValue != null) {
            OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                    refreshTokenValue,
                    rs.getTimestamp("refresh_token_issued_at").toInstant(),
                    rs.getTimestamp("refresh_token_expires_at").toInstant()
            );
            builder.token(refreshToken, metadata -> {
                try {
                    parseTokenMetadata(
                            rs.getString("refresh_token_metadata"),
                            metadata
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return builder.build();
    }

    /**
     * Parse token metadata from JSON string
     */
    private void parseTokenMetadata(String metadataStr, Map<String, Object> metadata) {
        if (StringUtils.hasText(metadataStr)) {
            try {
                Map<String, Object> jsonMap = new ObjectMapper()
                        .readValue(metadataStr, new TypeReference<Map<String, Object>>() {});
                metadata.putAll(jsonMap);
            } catch (JsonProcessingException e) {
                log.error("Error parsing token metadata", e);
            }
        }
    }

    /**
     * Resolve authorization grant type from string
     */
    private AuthorizationGrantType resolveAuthorizationGrantType(String grantType) {
        if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(grantType)) {
            return AuthorizationGrantType.AUTHORIZATION_CODE;
        } else if (AuthorizationGrantType.CLIENT_CREDENTIALS.getValue().equals(grantType)) {
            return AuthorizationGrantType.CLIENT_CREDENTIALS;
        } else if (AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(grantType)) {
            return AuthorizationGrantType.REFRESH_TOKEN;
        }
        return new AuthorizationGrantType(grantType);
    }

    // 新增方法：根据用户名查找所有授权
//    @Cacheable(key = "'principal:' + #principalName", unless = "#result == null || #result.isEmpty()")
    public List<OAuth2Authorization> findByPrincipalName(String principalName) {
        String sql = """
            SELECT id, registered_client_id, principal_name, authorization_grant_type, 
                   authorized_scopes, attributes, state, authorization_code_value, 
                   authorization_code_issued_at, authorization_code_expires_at,
                   authorization_code_metadata, access_token_value, 
                   access_token_issued_at, access_token_expires_at,
                   access_token_metadata, access_token_type, 
                   access_token_scopes, refresh_token_value,
                   refresh_token_issued_at, refresh_token_expires_at, 
                   refresh_token_metadata
            FROM oauth2_authorization 
            WHERE principal_name = ?
            """;

        return jdbcTemplate.query(sql, 
            this::extractAuthorization, 
            principalName
        );
    }

    public void save(OAuth2Authorization authorization) {
        super.save(authorization);
    }

    @Override
//    @Caching(evict = {@CacheEvict(key = "#authorization.id"), @CacheEvict(key = "'principal:' + #authorization.principalName")})
    public void remove(OAuth2Authorization authorization) {
        super.remove(authorization);
    }

    // Token管理相关方法
    public void revokeTokensByPrincipalName(String principalName) {
        List<OAuth2Authorization> authorizations = findByPrincipalName(principalName);
        for (OAuth2Authorization authorization : authorizations) {
            remove(authorization);
        }
    }

    public void revokeTokensByClientAndPrincipal(String clientId, String principalName) {
        String sql = """
            SELECT * FROM oauth2_authorization 
            WHERE registered_client_id = ? AND principal_name = ?
            """;
            
        List<OAuth2Authorization> authorizations = jdbcTemplate.query(
            sql,
            this::extractAuthorization,
            clientId,
            principalName
        );
        
        for (OAuth2Authorization authorization : authorizations) {
            remove(authorization);
        }
    }
}
