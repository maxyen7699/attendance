package com.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OvertimeApplicationRequest {

    @NotNull(message = "加班類型不可為空")
    private String type;

    @NotNull(message = "加班日期不可為空")
    private LocalDate overtimeDate;

    @NotNull(message = "開始時間不可為空")
    private LocalTime startTime;

    @NotNull(message = "結束時間不可為空")
    private LocalTime endTime;

    private BigDecimal hours;

    @NotNull(message = "加班原因不可為空")
    private String reason;
}
