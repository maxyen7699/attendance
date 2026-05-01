package com.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveApplicationRequest {
    @NotNull(message = "請假類型不可為空")
    private Long leaveTypeId;

    @NotNull(message = "開始日期不可為空")
    private LocalDate startDate;

    @NotNull(message = "結束日期不可為空")
    private LocalDate endDate;

    @NotNull(message = "請假天數不可為空")
    private BigDecimal totalDays;

    @NotNull(message = "請假原因不可為空")
    private String reason;

    private Long agentId;
}
