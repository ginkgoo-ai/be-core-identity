package com.benwk.ginkgoocoreidentity.controller;

import com.benwk.ginkgoocoreidentity.config.JwtTokenProvider;
import com.benwk.ginkgoocoreidentity.domain.User;
import com.benwk.ginkgoocoreidentity.repository.UserRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
public class UserApiController {
    @Resource
    UserRepository userRepository;

    @Resource
    JwtTokenProvider jwtTokenProvider;

    @GetMapping("/user/info")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String authHeader) {

        try {
            // 从 Authorization header 中提取 token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String token = authHeader.substring(7);

            // 验证 token
            if (!jwtTokenProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 从 token 中获取用户信息
            String email = jwtTokenProvider.getEmailFromToken(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            return ResponseEntity.ok(user);

        } catch (Exception e) {
            log.error("Error getting user info", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}