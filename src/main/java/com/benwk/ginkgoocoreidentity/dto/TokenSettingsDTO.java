package com.benwk.ginkgoocoreidentity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenSettingsDTO {
    private long accessTokenTimeToLive;
    private long refreshTokenTimeToLive;
    private boolean reuseRefreshTokens;
}