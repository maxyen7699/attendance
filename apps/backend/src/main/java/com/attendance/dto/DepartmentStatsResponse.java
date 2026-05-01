package com.attendance.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DepartmentStatsResponse {
    private String department;
    private Integer totalEmployees;
    private Double attendanceRate;         // 出勤率
    private BigDecimal avgLeaveDays;       // 平均請假天數
    private BigDecimal avgOvertimeHours;   // 平均加班時數
    private Integer lateCount;
}
