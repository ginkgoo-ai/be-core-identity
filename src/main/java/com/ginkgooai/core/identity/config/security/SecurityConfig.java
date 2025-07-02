package com.ginkgooai.core.identity.config.security;

import com.ginkgooai.core.identity.filter.MfaAuthenticationFilter;
import com.ginkgooai.core.identity.handler.FederatedIdentityAuthenticationSuccessHandler;
import com.ginkgooai.core.identity.handler.TokenRevocationLogoutHandler;
import com.ginkgooai.core.identity.handler.UserRepositoryOAuth2UserHandler;
import com.ginkgooai.core.identity.repository.DatabaseClientRegistrationRepository;
import com.ginkgooai.core.identity.repository.UserRepository;
import com.ginkgooai.core.identity.repository.UserSocialConnectionRepository;
import com.ginkgooai.core.identity.security.AdminApiKeyFilter;
import com.ginkgooai.core.identity.security.AdminIpFilter;
import com.ginkgooai.core.identity.security.AdminRateLimitFilter;
import com.ginkgooai.core.identity.service.CustomOAuth2UserService;
import com.ginkgooai.core.identity.service.CustomOidcUserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final UserSocialConnectionRepository socialConnectionRepository;

    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final JwtDecoder jwtDecoder;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final UserDetailsService userDetailsService;
    private final DatabaseClientRegistrationRepository clientRegistrationRepository;
    private final PasswordEncoder passwordEncoder;

    private final AdminIpFilter adminIpFilter;
    private final AdminApiKeyFilter adminApiKeyFilter;
    private final AdminRateLimitFilter adminRateLimitFilter;
    private final MfaAuthenticationFilter mfaAuthenticationFilter;


    @Bean
    public UserRepositoryOAuth2UserHandler oauth2UserHandler() {
        return new UserRepositoryOAuth2UserHandler(userRepository, socialConnectionRepository);
    }

    @Bean
    public FederatedIdentityAuthenticationSuccessHandler authenticationSuccessHandler() {
        FederatedIdentityAuthenticationSuccessHandler handler =
                new FederatedIdentityAuthenticationSuccessHandler();

        handler.setOAuth2UserHandler(oauth2UserHandler());
        handler.setOidcUserHandler(oidcUser -> oauth2UserHandler().accept(oidcUser));

        return handler;
    }
    

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
        return new HttpSessionOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        return new CustomOidcUserService(customOAuth2UserService);
    }

    private HttpSecurity configureResourceServer(HttpSecurity http) throws Exception {

        http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter))
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write(String.format(
                            "{\"error\": \"unauthorized\", \"message\": \"%s\"}",
                            authException.getMessage()
                    ));
                }));

        return http;
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain adminApiSecurityFilterChain(HttpSecurity http) throws Exception {
        return configureResourceServer(http)
			.securityMatcher("/admin/**")
			.cors(Customizer.withDefaults())
			.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/admin/**")
				.permitAll()
                )
                .anonymous(anonymous -> anonymous.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .sessionRegistry(sessionRegistry())
                )
                // Add custom filters
                .addFilterBefore(adminIpFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(adminApiKeyFilter, AdminIpFilter.class)
                .addFilterAfter(adminRateLimitFilter, AdminApiKeyFilter.class)
                // Exception handling
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(String.format(
                                    "{\"error\": \"Unauthorized\", \"message\": \"%s\"}",
                                    authException.getMessage()
                            ));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(String.format(
                                    "{\"error\": \"Forbidden\", \"message\": \"%s\"}",
                                    accessDeniedException.getMessage()
                            ));
                        })
                ).build();
    }


    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, TokenRevocationLogoutHandler tokenRevocationLogoutHandler) throws Exception {
        return configureResourceServer(http)
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				.ignoringRequestMatchers("/oauth2/authorize")
                )
                .authorizeHttpRequests(authorize -> authorize
                        //View endpoint
				.requestMatchers("/static/**", "/static/images/**", "/.well-known/**")
				.permitAll()
                        //OAuth2 endpoints
                        .requestMatchers("/oauth2/authorize",
//                                "oauth2/consent",
                                "/connect/logout",
                                "/api/oauth2/clients",
                                "/oauth2/token",
                                "/login",
                                "/logout",
                                "/reset-password",
                                "/verify-email")
                        .permitAll()
                        //Swagger endpoints
                        .requestMatchers(
                                "/api/identity/v3/api-docs",
                                "/api/identity/swagger-ui.html",
                                "/api/identity/swagger-ui/**",
                                "/v3/api-docs",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/webjars/**"
                        ).permitAll()
                        //health endpoints
                        .requestMatchers(
                                "/health"
                        ).permitAll()
                        //API endpoints
                        .requestMatchers(HttpMethod.POST, "/users").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/users/*/email/verification").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/password-resets").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/users/password-resets/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/users").authenticated()
                        .anyRequest().hasAnyRole("USER", "ADMIN")
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .clientRegistrationRepository(clientRegistrationRepository)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(oidcUserService())
                        )
                        .successHandler(authenticationSuccessHandler())
                )
                .oauth2Client(oauth2Client -> oauth2Client
                        .authorizationCodeGrant(codeGrant -> codeGrant
                                .authorizationRequestRepository(authorizationRequestRepository())
                        )
                )
                .formLogin(form -> form
                        .loginPage("/login")
				.loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
				.failureUrl("/login?error=true")
                        .permitAll()
                )
			.rememberMe(Customizer.withDefaults())
                .addFilterBefore(mfaAuthenticationFilter,  UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .addLogoutHandler(tokenRevocationLogoutHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .deleteCookies("SESSION")
                        .permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new ProblemDetailsAuthenticationEntryPoint())
                        .accessDeniedHandler(new ProblemDetailsAuthenticationEntryPoint())
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter())))
                .build();
    }

    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();

        jwtConverter.setPrincipalClaimName("email");
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            List<String> roles = getClaimAsList(jwt, "role");
            if (roles != null) {
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority(role.toUpperCase()));
                }
            }

            List<String> scopes = getClaimAsList(jwt, "scope");
            if (scopes != null) {
                for (String scope : scopes) {
                    authorities.add(new SimpleGrantedAuthority(scope));
                }
            }

            return authorities;
        });

        return jwtConverter;
    }

    private List<String> getClaimAsList(Jwt jwt, String claimName) {
        Object claimValue = jwt.getClaim(claimName);

        if (claimValue == null) {
            return null;
        }

        if (claimValue instanceof List) {
            return (List<String>) claimValue;
        }

        if (claimValue instanceof String) {
            return List.of(((String) claimValue).split(" "));
        }

        return null;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);

        resolver.setAuthorizationRequestCustomizer(customizer ->
                customizer.additionalParameters(params -> {
                    params.put("prompt", "select_account");
                    params.put("access_type", "offline");
                })
        );

        return resolver;
    }

    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);

        logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/login");

        return (request, response, authentication) -> {
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");
                response.getWriter().write("{\"redirectUrl\": \"/login\"}");
            } else {
                logoutSuccessHandler.onLogoutSuccess(request, response, authentication);
            }
        };
    }
}