package com.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OvertimeApplicationResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String type;
    private LocalDate overtimeDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal hours;
    private String reason;
    private String status;
    private Long approverId;
    private String approverName;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}
