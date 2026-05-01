package com.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String name;
    private String department;
    private String position;
    private Long supervisorId;
    private String role;
    private Integer annualLeaveDays;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
