package com.attendance.config;

import com.attendance.entity.User;
import com.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@attendance.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .name("System Administrator")
                    .role(User.Role.ADMIN)
                    .isActive(true)
                    .annualLeaveDays(14)
                    .build();
            userRepository.save(admin);
            log.info("Default admin account created (username: admin)");
        } else {
            log.info("Admin account already exists, skipping initialization");
        }
    }
}
