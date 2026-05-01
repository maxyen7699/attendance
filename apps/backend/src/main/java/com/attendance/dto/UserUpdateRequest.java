package com.attendance.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserUpdateRequest {
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    @Size(max = 50, message = "Department must be at most 50 characters")
    private String department;

    @Size(max = 50, message = "Position must be at most 50 characters")
    private String position;

    private Long supervisorId;

    private String role;

    private Integer annualLeaveDays;

    private Boolean isActive;
}
