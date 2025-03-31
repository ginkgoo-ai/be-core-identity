package com.ginkgooai.core.identity.domain.enums;

public enum LoginMethod {
	PASSWORD,      // Traditional password login
	OAUTH,        // OAuth Social login
	TEMP_TOKEN,    // Temporary token (for guest access)
	MFA            // Multi-factor authentication
}