package com.attendance.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MonthlyReportResponse {
    private Integer year;
    private Integer month;
    private Long userId;
    private String userName;
    private Integer totalWorkDays;        // 該月應上班天數（扣除週末）
    private Integer actualWorkDays;       // 實際打卡天數
    private Integer lateCount;            // 遲到次數
    private Integer earlyLeaveCount;      // 早退次數
    private BigDecimal leaveDays;         // 請假天數
    private BigDecimal overtimeHours;     // 加班時數
    private List<DailyRecord> dailyRecords;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DailyRecord {
        private String date;
        private String clockIn;
        private String clockOut;
        private String attendanceStatus;
        private String leaveType;
        private String leaveStatus;
    }
}
