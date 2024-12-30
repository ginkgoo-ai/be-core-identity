package com.ginkgooai.core.identity.dto.response;

import com.ginkgooai.core.identity.domain.enums.MfaType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MfaSendResponse {
    private MfaType type;
    private String additionalInfo;  // Additional information like TOTP QR code URL
}