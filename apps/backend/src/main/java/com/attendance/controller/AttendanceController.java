package com.attendance.controller;

import com.attendance.dto.*;
import com.attendance.repository.UserRepository;
import com.attendance.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserRepository userRepository;

    @PostMapping("/clock-in")
    public ResponseEntity<ApiResponse<ClockInResponse>> clockIn(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        ClockInResponse response = attendanceService.clockIn(userId);
        return ResponseEntity.ok(ApiResponse.success("上班打卡成功", response));
    }

    @PostMapping("/clock-out")
    public ResponseEntity<ApiResponse<ClockOutResponse>> clockOut(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        ClockOutResponse response = attendanceService.clockOut(userId);
        return ResponseEntity.ok(ApiResponse.success("下班打卡成功", response));
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<TodayStatusResponse>> getTodayStatus(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        TodayStatusResponse response = attendanceService.getTodayStatus(userId);
        return ResponseEntity.ok(ApiResponse.success("今日狀態查詢成功", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> getMyRecords(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<AttendanceRecordResponse> records = attendanceService.getMyRecords(userId);
        return ResponseEntity.ok(ApiResponse.success("個人紀錄查詢成功", records));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> getAllRecords(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        List<AttendanceRecordResponse> records = attendanceService.getAllRecords(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("全員紀錄查詢成功", records));
    }

    private Long getCurrentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
