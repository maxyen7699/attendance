package com.attendance.service;

import com.attendance.dto.UserRequest;
import com.attendance.dto.UserResponse;
import com.attendance.dto.UserUpdateRequest;
import com.attendance.entity.User;
import com.attendance.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        String rawPassword = generateRandomPassword();

        User.UserBuilder builder = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .name(request.getName())
                .department(request.getDepartment())
                .position(request.getPosition())
                .role(request.getRole() != null ? User.Role.valueOf(request.getRole()) : User.Role.USER)
                .annualLeaveDays(request.getAnnualLeaveDays() != null ? request.getAnnualLeaveDays() : 14)
                .isActive(true);

        if (request.getSupervisorId() != null) {
            User supervisor = userRepository.findById(request.getSupervisorId())
                    .orElseThrow(() -> new EntityNotFoundException("Supervisor not found with id: " + request.getSupervisorId()));
            builder.supervisor(supervisor);
        }

        User user = userRepository.save(builder.build());

        try {
            emailService.sendNewUserCredentials(user.getEmail(), user.getUsername(), rawPassword);
        } catch (Exception e) {
            log.warn("Failed to send credentials email to {}: {}", user.getEmail(), e.getMessage());
        }

        log.info("User created: {}", user.getUsername());
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null) {
            if (!request.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already exists: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }
        if (request.getPosition() != null) {
            user.setPosition(request.getPosition());
        }
        if (request.getRole() != null) {
            user.setRole(User.Role.valueOf(request.getRole()));
        }
        if (request.getAnnualLeaveDays() != null) {
            user.setAnnualLeaveDays(request.getAnnualLeaveDays());
        }
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        if (request.getSupervisorId() != null) {
            User supervisor = userRepository.findById(request.getSupervisorId())
                    .orElseThrow(() -> new EntityNotFoundException("Supervisor not found with id: " + request.getSupervisorId()));
            user.setSupervisor(supervisor);
        }

        User updated = userRepository.save(user);
        log.info("User updated: {}", updated.getUsername());
        return toResponse(updated);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        user.setIsActive(false);
        userRepository.save(user);
        log.info("User deactivated (soft delete): {}", user.getUsername());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .department(user.getDepartment())
                .position(user.getPosition())
                .supervisorId(user.getSupervisor() != null ? user.getSupervisor().getId() : null)
                .role(user.getRole().name())
                .annualLeaveDays(user.getAnnualLeaveDays())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 12);
    }
}
