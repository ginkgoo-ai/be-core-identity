package com.ginkgooai.core.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailVerificationRequest {
    @NotBlank
    private String verificationCode;
}