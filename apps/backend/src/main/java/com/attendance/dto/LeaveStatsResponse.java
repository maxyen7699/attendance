package com.attendance.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveStatsResponse {
    private String leaveTypeName;
    private Integer applicationCount;
    private BigDecimal totalDays;
    private Integer approvedCount;
    private Integer rejectedCount;
    private Integer pendingCount;
}
