package com.veltro.inventory.bootstrap;

import com.veltro.inventory.repository.UserRepository;
import com.veltro.inventory.util.PasswordHashUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@Order(1)
@Profile({"dev", "staging"})
@RequiredArgsConstructor
public class PasswordRehashRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Semillas exactas de V4_1__seed_dev_users.sql
    private static final Map<String, String> SEED_PASSWORDS = Map.of(
            "admin2", "admin123",
            "owner_test", "test123",
            "cashier_test", "test123"
    );

    @Override
    public void run(String... args) {
        SEED_PASSWORDS.forEach((username, knownPassword) -> {
            userRepository.findByUsername(username).ifPresent(user -> {
                // Comprobamos si TODAVÍA tiene el hash BCrypt del plaintext original
                if (passwordEncoder.matches(knownPassword, user.getPasswordHash())) {
                    String sha256 = PasswordHashUtils.sha256Hex(knownPassword);
                    user.setPasswordHash(passwordEncoder.encode(sha256));
                    userRepository.save(user);
                    log.info("Re-hashed seed user '{}' to BCrypt(SHA-256) format", username);
                }
            });
        });
    }
}
