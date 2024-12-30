package com.benwk.ginkgoocoreidentity.config.properties;

import com.benwk.ginkgoocoreidentity.enums.VerificationStrategy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.verification")
@Validated
public class VerificationProperties {
    @NotNull
    private VerificationStrategy strategy;
    
    @NotNull
    private CodeConfig code;
    
    @NotNull
    private UrlTokenConfig urlToken;
    
    @NotBlank
    private String redirectUrl;
    
    @Data
    public static class CodeConfig {
        private int length = 6;
        private long expiration = 300;
        private long cooldown = 60;
    }
    
    @Data
    public static class UrlTokenConfig {
        private long expiration = 900;
        private long cooldown = 60;
    }
    
    // Getters, setters, and validation...
}