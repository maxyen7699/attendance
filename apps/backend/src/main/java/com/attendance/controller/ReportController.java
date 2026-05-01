package com.attendance.controller;

import com.attendance.dto.*;
import com.attendance.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<MonthlyReportResponse>> getMonthlyReport(
            Authentication authentication,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Long userId = getCurrentUserId(authentication);
        MonthlyReportResponse response = reportService.getMonthlyReport(userId, year, month);
        return ResponseEntity.ok(ApiResponse.success("月報查詢成功", response));
    }

    @GetMapping("/department")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<DepartmentStatsResponse>>> getDepartmentStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        List<DepartmentStatsResponse> response = reportService.getDepartmentStats(year, month);
        return ResponseEntity.ok(ApiResponse.success("部門出勤統計查詢成功", response));
    }

    @GetMapping("/leave-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<LeaveStatsResponse>>> getLeaveStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        List<LeaveStatsResponse> response = reportService.getLeaveStats(year, month);
        return ResponseEntity.ok(ApiResponse.success("請假統計查詢成功", response));
    }

    @GetMapping("/overtime-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OvertimeStatsResponse>> getOvertimeStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        OvertimeStatsResponse response = reportService.getOvertimeStats(year, month);
        return ResponseEntity.ok(ApiResponse.success("加班統計查詢成功", response));
    }

    private Long getCurrentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
