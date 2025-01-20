package com.ginkgooai.core.identity.config.security;

import com.ginkgooai.core.identity.filter.MfaAuthenticationFilter;
import com.ginkgooai.core.identity.handler.FederatedIdentityAuthenticationSuccessHandler;
import com.ginkgooai.core.identity.handler.SpaCsrfTokenRequestHandler;
import com.ginkgooai.core.identity.handler.TokenRevocationLogoutHandler;
import com.ginkgooai.core.identity.handler.UserRepositoryOAuth2UserHandler;
import com.ginkgooai.core.identity.repository.DatabaseClientRegistrationRepository;
import com.ginkgooai.core.identity.repository.RoleRepository;
import com.ginkgooai.core.identity.repository.UserRepository;
import com.ginkgooai.core.identity.repository.UserSocialConnectionRepository;
import com.ginkgooai.core.identity.security.AdminApiKeyFilter;
import com.ginkgooai.core.identity.security.AdminIpFilter;
import com.ginkgooai.core.identity.security.AdminRateLimitFilter;
import com.ginkgooai.core.identity.service.CustomOAuth2UserService;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
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
        return new UserRepositoryOAuth2UserHandler(userRepository, roleRepository, socialConnectionRepository);
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
    @Order(2)
    public SecurityFilterChain adminApiSecurityFilterChain(HttpSecurity http) throws Exception {
        return configureResourceServer(http)
                .securityMatcher("/api/v1/oauth2/**")
                .cors(cors -> Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        .anyRequest().hasRole("ADMIN")
                )
                .anonymous(anonymous -> anonymous.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
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
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                )
                .authorizeHttpRequests(authorize -> authorize
                        //View endpoint
                        .requestMatchers("/oauth2/authorize",
                                "oauth2/consent",
                                "/api/oauth2/clients",
                                "/oauth2/token",
                                "/login",
                                "/logout",
                                "/reset-password",
                                "/verify-email")
                        .permitAll()
                        //Swagger endpoints
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/webjars/**"
                        ).permitAll()
                        //API endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/userinfos").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/userinfos/*/email/verification").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/userinfos/password-resets").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/userinfos/password-resets/*").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .clientRegistrationRepository(clientRegistrationRepository)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
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
                        .usernameParameter("email")
                        .passwordParameter("password")
                )
                .addFilterBefore(mfaAuthenticationFilter,  UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .addLogoutHandler(tokenRevocationLogoutHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                ).build();
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