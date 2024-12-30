package com.ginkgooai.core.identity.security;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "security.admin")
@Validated
@Getter
@Setter
public class AdminSecurityProperties {
    
    @NotEmpty
    private List<@Pattern(regexp = "^([0-9]{1,3}\\.){3}[0-9]{1,3}(/[0-9]{1,2})?$") String> ipWhitelist;
    
    @NotBlank
    private String apiKey;
    
    @Min(1)
    @Max(100)
    private int rateLimit = 10;
    
}