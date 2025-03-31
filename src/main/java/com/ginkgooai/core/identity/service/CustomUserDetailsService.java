package com.ginkgooai.core.identity.service;

import com.ginkgooai.core.identity.domain.UserInfo;
import com.ginkgooai.core.identity.domain.UserStatus;
import com.ginkgooai.core.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserInfo user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return new User(user.getEmail(), user.getPassword(), user.getStatus().equals(UserStatus.ACTIVE),
                true, true, true, user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role))
                .collect(Collectors.toList()));
    }
}