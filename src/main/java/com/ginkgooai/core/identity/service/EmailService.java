package com.ginkgooai.core.identity.service;

import com.ginkgooai.core.identity.config.properties.EmailProperties;
import com.ginkgooai.core.identity.enums.VerificationStrategy;
import com.ginkgooai.core.identity.exception.EmailSendException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    @Value("${app.verification.server-url}")
    private String serverBaseUrl;

//    @Autowired
//    private RegisteredClientRepository clientRepository;

    /**
     * Send verification email based on verification type
     * This method serves as a facade to choose the appropriate email template
     *
     * @param toEmail recipient's email address
     * @param credential verification code or URL token
     * @param strategy verification strategy being used
     */
    @Async
    public void sendVerificationEmail(String toEmail, String credential, String redirectUri, VerificationStrategy strategy) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailProperties.getFrom());
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Email Address");

            // Choose template based on strategy
            String emailContent = switch (strategy) {
                case CODE -> createVerificationCodeEmailContent(credential);
                case URL_TOKEN -> {
                    String verificationUrl = buildVerificationUrl(credential, redirectUri);
                    yield createVerificationUrlEmailContent(verificationUrl);
                }
            };

            helper.setText(emailContent, true);
            mailSender.send(message);

            log.info("Verification email ({}) sent successfully to: {}", strategy, toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new EmailSendException("Failed to send verification email");
        }
    }

    /**
     * Build verification URL with OAuth2.0 authorization flow support
     * After email verification, redirects to OAuth2.0 authorization endpoint
     *
     * @param token Verification token
     * @return Complete verification URL with OAuth2.0 parameters
     */
    private String buildVerificationUrl(String token, String redirectUri) {
        // First, build the verification success redirect URL (OAuth2.0 authorization endpoint)
        return UriComponentsBuilder
                .fromHttpUrl(serverBaseUrl)
                .path("/verify-email")
                .queryParam("token", token)
                .build()
                .toUriString();
    }

//    private String getClientRedirectUri(String clientId) {
//        RegisteredClient client = clientRepository.findByClientId(clientId);
//        if (client == null) {
//            throw new IllegalArgumentException("Invalid client_id: " + clientId);
//        }
//        // Assuming the first redirect URI is the main SPA URL
//        return client.getRedirectUris().iterator().next();
//    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailProperties.getFrom());
            helper.setTo(toEmail);
            helper.setSubject("Reset Your Password");
            helper.setText(createPasswordResetEmailContent(resetToken), true);

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new EmailSendException("Failed to send password reset email");
        }
    }

    @Async
    public void sendMfaSetupEmail(String toEmail, String qrCodeUri, String backupCodes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailProperties.getFrom());
            helper.setTo(toEmail);
            helper.setSubject("Your MFA Setup Information");
            helper.setText(createMfaSetupEmailContent(qrCodeUri, backupCodes), true);

            mailSender.send(message);
            log.info("MFA setup email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send MFA setup email to: {}", toEmail, e);
            throw new EmailSendException("Failed to send MFA setup email");
        }
    }

    @Async
    public void sendMfaCodeEmail(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailProperties.getFrom());
            helper.setTo(toEmail);
            helper.setSubject("Your MFA Verification Code");
            helper.setText(createMfaCodeEmailContent(code), true);

            mailSender.send(message);
            log.info("MFA code email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send MFA code email to: {}", toEmail, e);
            throw new EmailSendException("Failed to send MFA code email");
        }
    }

    private String createVerificationUrlEmailContent(String verificationUrl) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #ffffff;">
                <div style="text-align: center; margin-bottom: 30px;">
                    <h2 style="color: #1a1a1a; margin: 0;">Verify Your Email Address</h2>
                </div>
                <p style="color: #333333; font-size: 16px; line-height: 1.5;">Please click the button below to verify your email address:</p>
                <div style="text-align: center; margin: 30px 0;">
                    <a href="%s" style="display: inline-block; background-color: #4f46e5; color: white; padding: 12px 30px; text-decoration: none; border-radius: 6px; font-weight: 600;">
                        Verify Email
                    </a>
                </div>
                <p style="color: #666666; font-size: 14px;">Or copy and paste this URL into your browser:</p>
                <p style="color: #4f46e5; font-size: 14px; word-break: break-all;">%s</p>
                <p style="color: #333333; font-size: 14px; margin-top: 20px;">This link will expire in 5 minutes.</p>
                <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                <p style="color: #999999; font-size: 12px; text-align: center;">This is an automated message, please do not reply.</p>
            </div>
            """.formatted(verificationUrl, verificationUrl);
    }

    private String createVerificationCodeEmailContent(String code) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #ffffff;">
                <div style="text-align: center; margin-bottom: 30px;">
                    <h2 style="color: #1a1a1a; margin: 0;">Verify Your Email Address</h2>
                </div>
                <p style="color: #333333; font-size: 16px; line-height: 1.5;">Please use the following code to verify your email address:</p>
                <div style="background-color: #f8f9fa; border-radius: 6px; padding: 20px; margin: 20px 0; text-align: center;">
                    <h1 style="color: #333333; letter-spacing: 5px; margin: 0; font-size: 32px;">%s</h1>
                </div>
                <p style="color: #333333; font-size: 14px; margin-top: 20px;">This code will expire in 5 minutes.</p>
                <p style="color: #666666; font-size: 14px; margin-top: 30px;">If you didn't request this verification, please ignore this email.</p>
                <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                <p style="color: #999999; font-size: 12px; text-align: center;">This is an automated message, please do not reply.</p>
            </div>
            """.formatted(code);
    }

    private String createPasswordResetEmailContent(String resetToken) {
        String resetUrl = emailProperties.getAppUrl() + "/reset-password?token=" + resetToken;

        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #ffffff;">
                <div style="text-align: center; margin-bottom: 30px;">
                    <h2 style="color: #1a1a1a; margin: 0;">Reset Your Password</h2>
                </div>
                <p style="color: #333333; font-size: 16px; line-height: 1.5;">We received a request to reset your password. Click the button below to create a new password:</p>
                <div style="text-align: center; margin: 30px 0;">
                    <a href="%s" style="display: inline-block; background-color: #4f46e5; color: white; padding: 12px 30px; text-decoration: none; border-radius: 6px; font-weight: 600; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); transition: all 0.2s;">
                        Reset Password
                    </a>
                </div>
                <p style="color: #666666; font-size: 14px;">Or copy and paste this URL into your browser:</p>
                <p style="color: #4f46e5; font-size: 14px; word-break: break-all;">%s</p>
                <p style="color: #333333; font-size: 14px; margin-top: 20px;">This link will expire in 15 minutes.</p>
                <p style="color: #666666; font-size: 14px; margin-top: 30px;">If you didn't request this password reset, please ignore this email.</p>
                <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                <p style="color: #999999; font-size: 12px; text-align: center;">This is an automated message, please do not reply.</p>
            </div>
            """.formatted(resetUrl, resetUrl);
    }

    private String createMfaSetupEmailContent(String qrCodeUri, String backupCodes) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #ffffff;">
                <div style="text-align: center; margin-bottom: 30px;">
                    <h2 style="color: #1a1a1a; margin: 0;">Your MFA Setup Information</h2>
                </div>
                <p style="color: #333333; font-size: 16px; line-height: 1.5;">Please scan the QR code below with your authenticator app:</p>
                <div style="text-align: center; margin: 30px 0;">
                    <img src="%s" alt="QR Code" style="max-width: 200px; height: auto;"/>
                </div>
                <div style="background-color: #f8f9fa; border-radius: 6px; padding: 20px; margin: 20px 0;">
                    <h3 style="color: #1a1a1a; margin: 0 0 15px 0;">Your Backup Codes</h3>
                    <p style="color: #666666; font-size: 14px; margin-bottom: 15px;">Please save these codes in a secure location. Each code can only be used once.</p>
                    <pre style="background-color: #ffffff; padding: 15px; border-radius: 4px; font-family: monospace; margin: 0;">%s</pre>
                </div>
                <p style="color: #666666; font-size: 14px; margin-top: 30px;">If you didn't request MFA setup, please contact support immediately.</p>
                <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                <p style="color: #999999; font-size: 12px; text-align: center;">This is an automated message, please do not reply.</p>
            </div>
            """.formatted(qrCodeUri, backupCodes);
    }

    private String createMfaCodeEmailContent(String code) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #ffffff;">
                <div style="text-align: center; margin-bottom: 30px;">
                    <h2 style="color: #1a1a1a; margin: 0;">Your MFA Verification Code</h2>
                </div>
                <p style="color: #333333; font-size: 16px; line-height: 1.5;">Use this verification code to complete your login:</p>
                <div style="background-color: #f8f9fa; border-radius: 6px; padding: 20px; margin: 20px 0; text-align: center;">
                    <h1 style="color: #333333; letter-spacing: 5px; margin: 0; font-size: 32px;">%s</h1>
                </div>
                <p style="color: #333333; font-size: 14px; margin-top: 20px;">This code will expire in 5 minutes.</p>
                <p style="color: #666666; font-size: 14px; margin-top: 30px;">If you didn't try to log in, please change your password immediately.</p>
                <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                <p style="color: #999999; font-size: 12px; text-align: center;">This is an automated message, please do not reply.</p>
            </div>
            """.formatted(code);
    }

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("bJkgT1OgzUjViJDiOv0msKVBwqVy5IXCmtA1oGN0yB9bcR6CbRbpDEg5pLHHGkQFtZe7AqJ+1bWv+dDF3A96mg==");
        System.out.println(hash);
    }
}