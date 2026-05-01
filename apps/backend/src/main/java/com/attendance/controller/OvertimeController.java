package com.attendance.controller;

import com.attendance.dto.*;
import com.attendance.service.OvertimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/overtime")
@RequiredArgsConstructor
public class OvertimeController {

    private final OvertimeService overtimeService;

    @PostMapping
    public ResponseEntity<ApiResponse<OvertimeApplicationResponse>> applyOvertime(
            Authentication authentication,
            @Valid @RequestBody OvertimeApplicationRequest request) {
        Long userId = getCurrentUserId(authentication);
        OvertimeApplicationResponse response = overtimeService.applyOvertime(userId, request);
        return ResponseEntity.ok(ApiResponse.success("加班申請已提出", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<OvertimeApplicationResponse>>> getMyOvertime(
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<OvertimeApplicationResponse> response = overtimeService.getMyOvertime(userId);
        return ResponseEntity.ok(ApiResponse.success("查詢成功", response));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<OvertimeApplicationResponse>>> getPendingOvertime(
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<OvertimeApplicationResponse> response = overtimeService.getPendingOvertime(userId);
        return ResponseEntity.ok(ApiResponse.success("查詢成功", response));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<OvertimeApplicationResponse>> approveOvertime(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = getCurrentUserId(authentication);
        OvertimeApplicationResponse response = overtimeService.approveOvertime(id, userId);
        return ResponseEntity.ok(ApiResponse.success("簽核通過", response));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<OvertimeApplicationResponse>> rejectOvertime(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = getCurrentUserId(authentication);
        OvertimeApplicationResponse response = overtimeService.rejectOvertime(id, userId);
        return ResponseEntity.ok(ApiResponse.success("簽核駁回", response));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OvertimeStatsResponse>> getOvertimeStats(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        OvertimeStatsResponse response = overtimeService.getOvertimeStats(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("查詢成功", response));
    }

    private Long getCurrentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
