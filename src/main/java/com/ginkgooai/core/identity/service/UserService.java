package com.ginkgooai.core.identity.service;

import com.ginkgooai.core.common.enums.Role;
import com.ginkgooai.core.identity.domain.TokenIdentity;
import com.ginkgooai.core.identity.domain.UserInfo;
import com.ginkgooai.core.identity.domain.UserStatus;
import com.ginkgooai.core.identity.domain.enums.LoginMethod;
import com.ginkgooai.core.identity.dto.request.RegistrationRequest;
import com.ginkgooai.core.identity.dto.response.UserResponse;
import com.ginkgooai.core.identity.enums.VerificationStrategy;
import com.ginkgooai.core.identity.exception.*;
import com.ginkgooai.core.identity.repository.UserRepository;
import com.ginkgooai.core.identity.service.verification.EmailVerificationStrategy;
import com.ginkgooai.core.identity.service.verification.EmailVerificationStrategyFactory;
import com.ginkgooai.core.identity.specification.UserSpecification;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    static final String SAVED_REQUEST = "SPRING_SECURITY_SAVED_REQUEST";
    private final UserRepository userRepository;
    //    private final OAuth2RegisteredClientRepository clientRepository;
    private final VerificationCodeService verificationCodeService;
    private final EmailVerificationStrategyFactory strategyFactory;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.verification.strategy}")
    private VerificationStrategy defaultStrategy;

    @Transactional
    public UserResponse createUser(RegistrationRequest request, HttpServletRequest httpRequest) {
        log.debug("Creating new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Attempted to create user with existing email: {}", request.getEmail());
            throw new EmailAlreadyExistsException(String.format("User with email '%s' already exists", request.getEmail()));
        }

        String clientId = extractOauthClientSession(httpRequest, "client_id");
        String redirectUri = extractOauthClientSession(httpRequest, "redirect_uri");
        if (clientId == null) {
            log.warn("Client ID not found in request");
            throw new IllegalArgumentException("Client ID not found in request");
        }

//        OAuth2RegisteredClient client = clientRepository.findByClientId(clientId).orElseThrow(() ->
//                new ResourceNotFoundException("RegisteredClient", "client_id", clientId));
//
//        if (client.checkRedirectUrl(redirectUri)) {
//            log.warn("Invalid redirect URI for client ID: {}", clientId);
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid redirect URI");
//        }

        UserInfo user = new UserInfo();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRoles(new ArrayList<>());

        // Add default USER role
        user.getRoles().add(Role.ROLE_USER.name());

        UserInfo savedUser = userRepository.save(user);
        log.info("Successfully created user with email: {}", request.getEmail());

        // Send verification email based on strategy
        EmailVerificationStrategy verificationStrategy = strategyFactory.getStrategy(defaultStrategy);
        String credential = verificationStrategy.generateCredential(clientId, savedUser.getId());
        emailService.sendVerificationEmail(request.getEmail(), credential, redirectUri, defaultStrategy);

        return UserResponse.from(savedUser);
    }

    @Transactional
    public UserResponse createTempUser(RegistrationRequest request) {
        log.debug("Creating new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Attempted to create user with existing email: {}", request.getEmail());
            throw new EmailAlreadyExistsException(String.format("User with email '%s' already exists", request.getEmail()));
        }

        UserInfo user = new UserInfo();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(new ArrayList<>());
        user.getRoles().add(Role.ROLE_USER.name());
        user.setLoginMethods(new ArrayList<>());
        user.getLoginMethods().add(LoginMethod.TEMP_TOKEN.name());

        UserInfo savedUser = userRepository.save(user);
        log.info("Successfully created user with email: {}", request.getEmail());

        return UserResponse.from(savedUser);
    }

    /**
     * Extract client_id from saved request in session
     */
    private String extractOauthClientSession(HttpServletRequest request, String sessionKey) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            DefaultSavedRequest savedRequest = (DefaultSavedRequest) session.getAttribute(SAVED_REQUEST);
            if (savedRequest != null) {
                String queryString = savedRequest.getQueryString();
                if (queryString != null) {
                    MultiValueMap<String, String> parameters =
                        UriComponentsBuilder.newInstance()
                            .query(queryString)
                            .build()
                            .getQueryParams();
                    return parameters.getFirst(sessionKey);
                }
            }
        }
        return null;
    }


    @Transactional
    public UserResponse loadUser(String email) {
        log.debug("Retrieving user by email: {}", email);
        return UserResponse.from(userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email)));
    }

    public UserInfo getUserById(String userId) {
        log.debug("Retrieving user by ID: {}", userId);
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "ID", userId));
    }

    public List<UserResponse> getUsersByIds(List<String> userIds) {
        log.debug("Retrieving users by IDs: {}", userIds);
        List<UserInfo> users = userRepository.findAllById(userIds);
        return users.stream()
            .map(UserResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * Verify user's email using token from verification URL
     * Updates user status upon successful verification
     */
    @Transactional
    public TokenIdentity verifyEmailByToken(String token) throws InvalidVerificationCodeException {
        TokenIdentity tokenIdentity = verificationCodeService.verifyEmailToken(token);

        UserInfo user = userRepository.findById(tokenIdentity.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", tokenIdentity.getUserId()));

        if (user.isEmailVerified()) {
            log.info("Email already verified for user ID: {}", tokenIdentity.getUserId());
            return tokenIdentity;
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("Successfully verified email for user ID: {}", tokenIdentity.getUserId());

        return tokenIdentity;
    }

    @Transactional
    public void verifyEmail(String userId, String verificationCode) throws InvalidVerificationCodeException {
        log.debug("Verifying email for user ID: {}", userId);

        UserInfo user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.isEmailVerified()) {
            log.info("Email already verified for user ID: {}", userId);
            return;
        }

        if (!verificationCodeService.verifyCode(userId, verificationCode)) {
            log.warn("Invalid verification code provided for user ID: {}", userId);
            throw new InvalidVerificationCodeException(
                String.format("Invalid or expired verification code for user ID: %s", userId)
            );
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("Successfully verified email for user ID: {}", userId);
    }

    @Transactional
    public String regenerateVerificationToken(String userId) {
        log.debug("Regenerating verification token for user ID: {}", userId);

        UserInfo user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.isEmailVerified()) {
            log.warn("Attempted to regenerate verification code for already verified user: {}", userId);
            throw new EmailAlreadyVerifiedException(
                String.format("Email already verified for user ID: %s", userId)
            );
        }

        String newCode = verificationCodeService.generateCode(userId);
        log.info("Successfully regenerated verification code for user ID: {}", userId);
        return newCode;
    }

    public void updatePassword(String userId, String oldPassword, String newPassword) {
        UserInfo user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new InvalidPasswordException("Invalid old password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        log.debug("Initiating password reset for email: {}", email);

        UserInfo user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String resetToken = verificationCodeService.generatePasswordResetToken(user.getId());
        log.info("Generated password reset token for user ID: {}", user.getId());

        emailService.sendPasswordResetEmail(email, resetToken);
    }

    @Transactional
    public void confirmPasswordReset(String resetToken, String newPassword) throws InvalidVerificationCodeException {
        log.debug("Confirming password reset with token");

        // Extract user ID from token or maintain token-userId mapping
        String userId = verificationCodeService.getUserIdFromToken(resetToken);

        if (!verificationCodeService.verifyCode(userId, resetToken)) {
            log.warn("Invalid or expired password reset token");
            throw new InvalidVerificationCodeException("Invalid or expired password reset token");
        }

        UserInfo user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate the reset token
        verificationCodeService.invalidateCode(userId);

        log.info("Successfully reset password for user ID: {}", userId);
    }

    @Transactional
    public UserResponse patchUserInfo(@NotBlank String userId, String pictureUrl, String fistName, String lastName) {

        UserInfo user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setPassword(pictureUrl);
        user.setFirstName(fistName);
        user.setLastName(lastName);
        ;
        user.setStatus(UserStatus.ACTIVE);

        userRepository.updateSelective(user);

        return UserResponse.from(user);
    }

    /**
     * Find user by dynamic criteria using JPA Specification
     *
     * @param email Query specification
     * @return Matched user entity
     * @throws ResourceNotFoundException when no user found
     */
    public UserInfo getUserBySpecification(String email, String name) {
        Specification<UserInfo> spec = Specification.where(UserSpecification.findByEmail(email));

        // Apply name filter if provided and not empty
        if (name != null && !name.trim().isEmpty()) {
            spec = spec.and(UserSpecification.hasNameLike(name.trim()));
        }

        return userRepository.findOne(spec)
            .orElseThrow(() -> new ResourceNotFoundException("User", "criteria", ""));
    }

}