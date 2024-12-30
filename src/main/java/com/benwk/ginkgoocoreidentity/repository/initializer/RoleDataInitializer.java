package com.benwk.ginkgoocoreidentity.repository.initializer;

import com.benwk.ginkgoocoreidentity.domain.Role;
import com.benwk.ginkgoocoreidentity.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RoleDataInitializer implements ApplicationRunner {
    
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createRoleIfNotExists(Role.ROLE_USER);
        createRoleIfNotExists(Role.ROLE_ADMIN);
    }

    private void createRoleIfNotExists(String roleName) {
        if (!roleRepository.existsByName(roleName)) {
            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
        }
    }
}