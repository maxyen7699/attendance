package com.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OvertimeStatsResponse {
    private BigDecimal totalHours;
    private Long totalCount;
    private Long approvedCount;
    private Long rejectedCount;
    private Long pendingCount;
}
