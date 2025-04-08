package com.ginkgooai.core.identity.controller;

import com.ginkgooai.core.identity.domain.UserInfo;
import com.ginkgooai.core.identity.domain.UserStatus;
import com.ginkgooai.core.identity.domain.enums.LoginMethod;
import com.ginkgooai.core.identity.repository.UserRepository;
import com.ginkgooai.core.identity.service.ShareCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/share-codes")
public class ShareCodeController {

	@Autowired
	private ShareCodeService shareCodeService;

	@Autowired
	private UserRepository userRepository;

	@PostMapping
	public ResponseEntity<ShareCodeResponse> generateShareCode(
		@RequestBody ShareCodeRequest request) {
		if (request.resource() == null
			|| request.resourceId() == null
			|| request.guestEmail() == null
			|| request.roles() == null
			|| request.roles().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		int expiryHours = request.expiryHours() > 0 ? request.expiryHours() : 24;

		UserInfo user = userRepository.findByEmail(request.guestEmail()).orElseGet(() ->
			userRepository.save(UserInfo.builder()
				.email(request.guestEmail)
				.firstName(request.guestName.split(" ")[0])
				.lastName(request.guestName.split(" ")[1])
				.roles(request.roles)
				.loginMethods(List.of(LoginMethod.TEMP_TOKEN.name()))
				.status(UserStatus.ACTIVE)
				.build()));

		String shareCode =
			shareCodeService.generateShareCode(
				request.resource(),
				request.resourceId(),
				user.getId(),
				request.write(),
				expiryHours,
				request.workspaceId());

		Instant expiresAt = Instant.now().plusSeconds(expiryHours * 3600L);

		return ResponseEntity.ok(
			new ShareCodeResponse(
				shareCode, request.resourceId(), user.getId(), expiresAt.toString(), expiryHours));
	}

	@GetMapping("/validate")
	public ResponseEntity<?> validateShareCode(
		@RequestParam("code") String shareCode,
		@RequestParam("resource_id") String resourceId) {

		try {
			ShareCodeService.ShareCodeInfo codeInfo = shareCodeService.validateShareCode(shareCode);

			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("valid", true);
			responseMap.put("resource", codeInfo.resource());
			responseMap.put("resourceId", codeInfo.resourceId());
			responseMap.put("userId", codeInfo.userId());
			responseMap.put("write", codeInfo.write());
			responseMap.put("expiresAt", codeInfo.expiresAt().toString());

			if (codeInfo.workspaceId() != null) {
				responseMap.put("workspaceId", codeInfo.workspaceId());
			}

			return ResponseEntity.ok(responseMap);
		} catch (Exception e) {
			return ResponseEntity.ok(Map.of("valid", false, "error", e.getMessage()));
		}
	}

	@DeleteMapping("/{shareCode}")
	public ResponseEntity<Void> revokeShareCode(@PathVariable String shareCode) {
		shareCodeService.revokeShareCode(shareCode);

		return ResponseEntity.ok().build();
	}

	public record ShareCodeRequest(
		String resource,
		String resourceId,
		boolean write,
		String guestName,
		String guestEmail,
		List<String> roles,
		String redirectUrl,
		int expiryHours,
		String workspaceId) {
	}

	public record ShareCodeResponse(
		String shareCode, String resourceId, String userId, String expiresAt, int expiryHours) {
	}
}
