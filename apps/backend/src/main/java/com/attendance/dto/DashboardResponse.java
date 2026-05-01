package com.attendance.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardResponse {
    private Long totalEmployees;
    private Double todayAttendanceRate;
    private Integer todayLateCount;
    private Integer pendingLeaveCount;
    private Integer pendingOvertimeCount;
    private BigDecimal monthlyOvertimeHours;
    private BigDecimal monthlyLeaveDays;
    private List<RecentAnomaly> recentAnomalies;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RecentAnomaly {
        private Long userId;
        private String userName;
        private String date;
        private String type;     // LATE, EARLY_LEAVE, ABSENT, FORGOT
        private String detail;
    }
}
