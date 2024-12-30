package com.ginkgooai.core.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientSettingsDTO {
    private boolean requireAuthorizationConsent;
    private boolean requireProofKey;
}
