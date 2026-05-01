package com.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TodayStatusResponse {
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private String status;
    private Boolean hasClockedIn;
    private Boolean hasClockedOut;
}
