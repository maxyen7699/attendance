package com.attendance.config;

import com.attendance.entity.LeaveBalance;
import com.attendance.entity.LeaveType;
import com.attendance.entity.User;
import com.attendance.repository.LeaveBalanceRepository;
import com.attendance.repository.LeaveTypeRepository;
import com.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdmin();
        initLeaveTypes();
        initLeaveBalances();
    }

    private void initAdmin() {
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

    private void initLeaveTypes() {
        if (leaveTypeRepository.count() > 0) {
            log.info("Leave types already initialized, skipping");
            return;
        }

        List<LeaveType> defaultTypes = List.of(
                LeaveType.builder().name("annual").deductible(false).build(),
                LeaveType.builder().name("personal").deductible(true).build(),
                LeaveType.builder().name("sick").deductible(true).build(),
                LeaveType.builder().name("compensatory").deductible(false).build(),
                LeaveType.builder().name("special").deductible(false).build()
        );

        leaveTypeRepository.saveAll(defaultTypes);
        log.info("Default leave types created: {}", defaultTypes.stream().map(LeaveType::getName).toList());
    }

    private void initLeaveBalances() {
        int currentYear = LocalDate.now().getYear();

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            return;
        }

        List<LeaveType> leaveTypes = leaveTypeRepository.findAll();
        if (leaveTypes.isEmpty()) {
            return;
        }

        List<LeaveBalance> newBalances = new ArrayList<>();

        for (User user : users) {
            for (LeaveType leaveType : leaveTypes) {
                boolean alreadyExists = leaveBalanceRepository
                        .findByUserAndLeaveTypeAndYear(user, leaveType, currentYear)
                        .isPresent();
                if (alreadyExists) {
                    continue;
                }

                BigDecimal totalDays = getDefaultDays(leaveType.getName(), user);
                newBalances.add(LeaveBalance.builder()
                        .user(user)
                        .leaveType(leaveType)
                        .totalDays(totalDays)
                        .usedDays(BigDecimal.ZERO)
                        .remainingDays(totalDays)
                        .year(currentYear)
                        .build());
            }
        }

        if (!newBalances.isEmpty()) {
            leaveBalanceRepository.saveAll(newBalances);
            log.info("Initialized {} leave balance records for year {}", newBalances.size(), currentYear);
        }
    }

    private BigDecimal getDefaultDays(String leaveTypeName, User user) {
        return switch (leaveTypeName) {
            case "annual" -> BigDecimal.valueOf(user.getAnnualLeaveDays() != null ? user.getAnnualLeaveDays() : 14);
            case "personal" -> new BigDecimal("7.0");
            case "sick" -> new BigDecimal("30.0");
            case "compensatory" -> BigDecimal.ZERO;
            case "special" -> new BigDecimal("3.0");
            default -> BigDecimal.ZERO;
        };
    }
}
