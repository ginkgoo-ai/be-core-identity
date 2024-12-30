package com.benwk.ginkgoocoreidentity.dto.response;

import com.benwk.ginkgoocoreidentity.domain.enums.MfaType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MfaSendResponse {
    private MfaType type;
    private String additionalInfo;  // Additional information like TOTP QR code URL
}