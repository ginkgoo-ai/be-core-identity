package com.ginkgooai.core.identity.config.security;

import com.ginkgooai.core.identity.dto.UserInfoAuthentication;
import com.ginkgooai.core.identity.dto.response.UserResponse;
import com.ginkgooai.core.identity.exception.ResourceNotFoundException;
import com.ginkgooai.core.identity.handler.CustomLogoutSuccessHandler;
import com.ginkgooai.core.identity.security.FederatedIdentityIdTokenCustomizer;
import com.ginkgooai.core.identity.security.ShareCodeGrantAuthenticationConverter;
import com.ginkgooai.core.identity.security.ShareCodeGrantAuthenticationProvider;
import com.ginkgooai.core.identity.service.ShareCodeService;
import com.ginkgooai.core.identity.service.UserService;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
@EnableWebSecurity
@Slf4j
public class AuthorizationServerConfig {

    @Autowired
    private UserService userService;

    @Value("${app.auth-server-uri}")
    private String authServerUrl;

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(authServerUrl)
                .build();
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> idTokenCustomizer(UserService userService) {
        return new FederatedIdentityIdTokenCustomizer(userService);
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(
                jdbcTemplate,
                registeredClientRepository);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http,
            OAuth2AuthorizationService authorizationService,
            OAuth2TokenGenerator<?> tokenGenerator,
                                                                      ShareCodeService shareCodeService,
            CustomLogoutSuccessHandler customLogoutSuccessHandler)
            throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer
                .authorizationServer();
        http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .cors(Customizer.withDefaults())
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, (authorizationServer) -> authorizationServer
                        // .authorizationEndpoint(endpoint ->
                        // endpoint.consentPage("/oauth2/consent")
                        // )
                        .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                                .accessTokenRequestConverter(
                                    new ShareCodeGrantAuthenticationConverter())
                                .authenticationProvider(
                                    new ShareCodeGrantAuthenticationProvider(
                                                authorizationService,
                                                tokenGenerator,
                                        shareCodeService,
                                        userService)))
                        .oidc(oidc -> oidc
                                .userInfoEndpoint(userInfo -> userInfo
                                        .userInfoMapper(userInfoMapperWithCustomClaims()))
                                .logoutEndpoint(logout -> logout
                                        .logoutResponseHandler(customLogoutSuccessHandler))
                                .clientRegistrationEndpoint(Customizer.withDefaults()))

                )
                .authorizeHttpRequests((authorize) -> authorize
                        .anyRequest().authenticated())
                // Redirect to the OAuth 2.0 Login endpoint when not authenticated
                .exceptionHandling((exceptions) -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                // new LoginUrlAuthenticationEntryPoint("http://127.0.0.1:4000/login"),
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));

        return http.build();
    }

    @Bean
    OAuth2TokenGenerator<?> tokenGenerator(
            JWKSource<SecurityContext> jwkSource,
            OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer) {

        JwtGenerator jwtGenerator = new JwtGenerator(new NimbusJwtEncoder(jwkSource));
        jwtGenerator.setJwtCustomizer(tokenCustomizer);

        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();

        return new DelegatingOAuth2TokenGenerator(
                jwtGenerator, accessTokenGenerator, refreshTokenGenerator);
    }

    public Function<OidcUserInfoAuthenticationContext, OidcUserInfo> userInfoMapperWithCustomClaims() {
        return context -> {
            OAuth2Authorization authorization = context.getAuthorization();
            String email = extractEmail(authorization);

            if (email == null) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_token", "Email claim not found in token", null));
            }

			try {
				UserResponse userResponse = userService.loadUser(email);
				String sub = authorization.getAccessToken().getClaims().get("sub").toString();
				return buildOidcUserInfo(userResponse, sub);
			}
			catch (ResourceNotFoundException e) {
				// Log the error for debugging but don't interrupt the authentication flow
				log.warn("User not found during UserInfo mapping for email: {}", email);

				// Throw a standard OAuth2 authentication exception that will be handled
				// by the framework
				// to redirect users back to the login page with proper error handling
				throw new OAuth2AuthenticationException(
						new OAuth2Error("invalid_user", "User not found with email: " + email, null));
			}
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
                return user.getUsername(); // email as username login
            }
        }

        Map<String, Object> claims = authorization.getAccessToken().getClaims();
        if (claims != null && claims.containsKey("email")) {
            return (String) claims.get("email");
        }

        return null;
    }

    private OidcUserInfo buildOidcUserInfo(UserResponse userResponse, String sub) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userResponse.getId());
        claims.put("sub", sub);
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