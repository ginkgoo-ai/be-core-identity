package com.benwk.ginkgoocoreidentity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(name = "BackupCodesResponse", description = "Response object containing backup codes for MFA recovery")
public class BackupCodesResponse {
    @Schema(
        title = "Backup Codes",
        description = "List of one-time use backup codes",
        example = "[\"12345678\", \"90123456\"]"
    )
    private List<String> backupCodes;
}