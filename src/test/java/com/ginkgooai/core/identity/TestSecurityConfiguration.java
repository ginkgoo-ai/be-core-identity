package com.ginkgooai.core.identity;

import com.ginkgooai.core.common.security.CustomGrantTypes;
import com.ginkgooai.core.identity.service.GuestCodeService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;

import java.util.UUID;

@TestConfiguration
public class TestSecurityConfiguration {

    @Bean
    @Primary
    public GuestCodeService guestCodeService() {
        return Mockito.mock(GuestCodeService.class);
    }
    
    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/identity/guest-codes").permitAll()
                .requestMatchers("/oauth2/token").permitAll()
                .anyRequest().permitAll()
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(httpBasic -> {})
            .anonymous(anonymous -> {});
            
        return http.build();
    }
    
    @Bean
    @Primary
    public RegisteredClientRepository testRegisteredClientRepository() {
        RegisteredClient sharingServiceClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("sharing-service")
                .clientSecret("{noop}sharing-service-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(CustomGrantTypes.GUEST_CODE)
                .scope("guest_code.create")
                .scope("guest_code.validate")
                .build();
                
        return new InMemoryRegisteredClientRepository(sharingServiceClient);
    }
}
