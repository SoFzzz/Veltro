package com.veltro.inventory.infrastructure.bootstrap;

import com.veltro.inventory.domain.iam.model.Role;
import com.veltro.inventory.domain.iam.model.UserEntity;
import com.veltro.inventory.domain.iam.ports.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class DevDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsernameAndActiveTrue("admin").isEmpty()) {
            UserEntity admin = new UserEntity();
            admin.setUsername("admin");
            admin.setEmail("admin@veltro.dev");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            admin.setCreatedBy("SYSTEM");
            userRepository.save(admin);
            System.out.println("✅ Default admin user created (admin/admin123)");
        }
    }
}
