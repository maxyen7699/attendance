package com.attendance.controller;

import com.attendance.dto.*;
import com.attendance.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> applyLeave(
            Authentication authentication,
            @Valid @RequestBody LeaveApplicationRequest request) {
        Long userId = getCurrentUserId(authentication);
        LeaveApplicationResponse response = leaveService.applyLeave(userId, request);
        return ResponseEntity.ok(ApiResponse.success("請假申請已提出", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<LeaveApplicationResponse>>> getMyLeaves(
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<LeaveApplicationResponse> response = leaveService.getMyLeaves(userId);
        return ResponseEntity.ok(ApiResponse.success("查詢成功", response));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal != null")
    public ResponseEntity<ApiResponse<List<LeaveApplicationResponse>>> getPendingApprovals(
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<LeaveApplicationResponse> response = leaveService.getPendingApprovals(userId);
        return ResponseEntity.ok(ApiResponse.success("查詢成功", response));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> approveLeave(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = getCurrentUserId(authentication);
        LeaveApplicationResponse response = leaveService.approveLeave(id, userId);
        return ResponseEntity.ok(ApiResponse.success("簽核通過", response));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> rejectLeave(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = getCurrentUserId(authentication);
        LeaveApplicationResponse response = leaveService.rejectLeave(id, userId);
        return ResponseEntity.ok(ApiResponse.success("簽核駁回", response));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> cancelLeave(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = getCurrentUserId(authentication);
        LeaveApplicationResponse response = leaveService.cancelLeave(id, userId);
        return ResponseEntity.ok(ApiResponse.success("請假已取消", response));
    }

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>> getLeaveBalances(
            Authentication authentication,
            @RequestParam(required = false) Integer year) {
        Long userId = getCurrentUserId(authentication);
        List<LeaveBalanceResponse> response = leaveService.getLeaveBalances(userId, year);
        return ResponseEntity.ok(ApiResponse.success("查詢成功", response));
    }

    @GetMapping("/proxy")
    public ResponseEntity<ApiResponse<List<LeaveApplicationResponse>>> getProxyTasks(
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<LeaveApplicationResponse> response = leaveService.getProxyTasks(userId);
        return ResponseEntity.ok(ApiResponse.success("查詢成功", response));
    }

    private Long getCurrentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
