package com.benwk.ginkgoocoreidentity.config.security;

import com.benwk.ginkgoocoreidentity.dto.UserInfoAuthentication;
import com.benwk.ginkgoocoreidentity.dto.response.UserResponse;
import com.benwk.ginkgoocoreidentity.security.FederatedIdentityIdTokenCustomizer;
import com.benwk.ginkgoocoreidentity.service.UserService;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class AuthorizationServerConfig {

    private final UserService userService;


    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:9000")
                .build();
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> idTokenCustomizer() {
        return new FederatedIdentityIdTokenCustomizer();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(
                jdbcTemplate,
                registeredClientRepository
        );
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();
        http.securityMatcher("/oauth2/**", "/login/oauth2/**")
                .cors(Customizer.withDefaults())
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, (authorizationServer) ->
                        authorizationServer
                                .authorizationEndpoint(endpoint ->
                                        endpoint.consentPage("/oauth2/consent")
                                )
                                .oidc(oidc -> oidc
                                        .userInfoEndpoint(userInfo -> userInfo
                                                .userInfoMapper(userInfoMapperWithCustomClaims())
                                        )
                                )

                )
                .authorizeHttpRequests((authorize) ->
                        authorize
                                .anyRequest().authenticated()
                )
                // Redirect to the OAuth 2.0 Login endpoint when not authenticated
                .exceptionHandling((exceptions) -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                );

        return http.build();
    }


    public Function<OidcUserInfoAuthenticationContext, OidcUserInfo> userInfoMapperWithCustomClaims() {
        return context -> {
            OAuth2Authorization authorization = context.getAuthorization();
            String email = extractEmail(authorization);

            if (email == null) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_token", "Email claim not found in token", null)
                );
            }

            UserResponse userResponse = userService.getUser(email);
            if (userResponse == null) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_user", "User not found with email: " + email, null)
                );
            }

            return buildOidcUserInfo(userResponse);
        };
    }

    private String extractEmail(OAuth2Authorization authorization) {
        Object principal = authorization.getAttribute("java.security.Principal");

        // OAuth2 social login
        if (principal instanceof OAuth2AuthenticationToken token) {
            OAuth2User oauth2User = token.getPrincipal();
            return oauth2User.getAttribute("email");
        }

        // Form login 
        if (principal instanceof UsernamePasswordAuthenticationToken token) {
            Object userDetails = token.getPrincipal();

            if (userDetails instanceof UserInfoAuthentication userInfo) {
                return userInfo.getEmail();
            }

            if (userDetails instanceof UserDetails user) {
                return user.getUsername(); //email as username login
            }
        }

        Map<String, Object> claims = authorization.getAccessToken().getClaims();
        if (claims != null && claims.containsKey("email")) {
            return (String) claims.get("email");
        }

        return null;
    }

    private OidcUserInfo buildOidcUserInfo(UserResponse userResponse) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userResponse.getId().toString());
        claims.put("email", userResponse.getEmail());
        claims.put("email_verified", true);
        claims.put("first_name", userResponse.getFirstName());
        claims.put("last_name", userResponse.getLastName());
        claims.put("name", userResponse.getFirstName() + " " + userResponse.getLastName());

        if (userResponse.getRoles() != null) {
            claims.put("roles", userResponse.getRoles());
        }

        return new OidcUserInfo(claims);
    }


}