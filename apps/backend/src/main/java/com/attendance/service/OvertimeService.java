package com.attendance.service;

import com.attendance.dto.OvertimeApplicationRequest;
import com.attendance.dto.OvertimeApplicationResponse;
import com.attendance.dto.OvertimeStatsResponse;
import com.attendance.entity.*;
import com.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OvertimeService {

    private final OvertimeApplicationRepository overtimeApplicationRepository;
    private final UserRepository userRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    @Transactional
    public OvertimeApplicationResponse applyOvertime(Long userId, OvertimeApplicationRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("使用者不存在"));

        User approver = user.getSupervisor();
        if (approver == null) {
            throw new RuntimeException("未設定主管，無法提出加班申請");
        }

        BigDecimal hours = req.getHours();
        if (hours == null) {
            long minutes = Duration.between(req.getStartTime(), req.getEndTime()).toMinutes();
            hours = BigDecimal.valueOf(minutes)
                    .divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
        }

        OvertimeApplication.OvertimeType type = OvertimeApplication.OvertimeType.valueOf(req.getType());

        OvertimeApplication application = OvertimeApplication.builder()
                .user(user)
                .type(type)
                .overtimeDate(req.getOvertimeDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .hours(hours)
                .reason(req.getReason())
                .status(OvertimeApplication.OvertimeStatus.PENDING)
                .approver(approver)
                .build();

        OvertimeApplication saved = overtimeApplicationRepository.save(application);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OvertimeApplicationResponse> getMyOvertime(Long userId) {
        return overtimeApplicationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OvertimeApplicationResponse> getPendingOvertime(Long userId) {
        return overtimeApplicationRepository
                .findByApproverIdAndStatus(userId, OvertimeApplication.OvertimeStatus.PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OvertimeApplicationResponse approveOvertime(Long applicationId, Long approverId) {
        OvertimeApplication application = overtimeApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("加班申請不存在"));

        if (application.getStatus() != OvertimeApplication.OvertimeStatus.PENDING) {
            throw new RuntimeException("此申請目前非待簽核狀態");
        }

        if (application.getApprover() == null || !application.getApprover().getId().equals(approverId)) {
            throw new RuntimeException("您無權簽核此加班申請");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("簽核者不存在"));

        application.setStatus(OvertimeApplication.OvertimeStatus.APPROVED);
        application.setApprover(approver);
        application.setApprovedAt(LocalDateTime.now());

        // Compensatory leave settlement: hours / 8 = compensatory days
        BigDecimal compensatoryDays = application.getHours()
                .divide(BigDecimal.valueOf(8), 1, RoundingMode.HALF_UP);

        if (compensatoryDays.compareTo(BigDecimal.ZERO) > 0) {
            int year = application.getOvertimeDate().getYear();
            LeaveType compLeaveType = leaveTypeRepository.findByName("補休")
                    .orElseThrow(() -> new RuntimeException("補休假別不存在，請先建立"));

            LeaveBalance balance = leaveBalanceRepository
                    .findByUserAndLeaveTypeAndYear(application.getUser(), compLeaveType, year)
                    .orElse(null);

            if (balance == null) {
                balance = LeaveBalance.builder()
                        .user(application.getUser())
                        .leaveType(compLeaveType)
                        .totalDays(compensatoryDays)
                        .usedDays(BigDecimal.ZERO)
                        .remainingDays(compensatoryDays)
                        .year(year)
                        .build();
            } else {
                balance.setTotalDays(balance.getTotalDays().add(compensatoryDays));
                balance.setRemainingDays(balance.getTotalDays().subtract(balance.getUsedDays()));
            }
            leaveBalanceRepository.save(balance);
        }

        OvertimeApplication saved = overtimeApplicationRepository.save(application);
        return toResponse(saved);
    }

    @Transactional
    public OvertimeApplicationResponse rejectOvertime(Long applicationId, Long approverId) {
        OvertimeApplication application = overtimeApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("加班申請不存在"));

        if (application.getStatus() != OvertimeApplication.OvertimeStatus.PENDING) {
            throw new RuntimeException("此申請目前非待簽核狀態");
        }

        if (application.getApprover() == null || !application.getApprover().getId().equals(approverId)) {
            throw new RuntimeException("您無權簽核此加班申請");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("簽核者不存在"));

        application.setStatus(OvertimeApplication.OvertimeStatus.REJECTED);
        application.setApprover(approver);
        application.setApprovedAt(LocalDateTime.now());

        OvertimeApplication saved = overtimeApplicationRepository.save(application);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OvertimeStatsResponse getOvertimeStats(LocalDate startDate, LocalDate endDate) {
        List<OvertimeApplication> applications;
        if (startDate != null && endDate != null) {
            applications = overtimeApplicationRepository
                    .findByOvertimeDateBetweenOrderByOvertimeDateDesc(startDate, endDate);
        } else {
            applications = overtimeApplicationRepository.findAll();
        }

        BigDecimal totalHours = applications.stream()
                .map(OvertimeApplication::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long approvedCount = applications.stream()
                .filter(a -> a.getStatus() == OvertimeApplication.OvertimeStatus.APPROVED)
                .count();

        long rejectedCount = applications.stream()
                .filter(a -> a.getStatus() == OvertimeApplication.OvertimeStatus.REJECTED)
                .count();

        long pendingCount = applications.stream()
                .filter(a -> a.getStatus() == OvertimeApplication.OvertimeStatus.PENDING)
                .count();

        return OvertimeStatsResponse.builder()
                .totalHours(totalHours)
                .totalCount((long) applications.size())
                .approvedCount(approvedCount)
                .rejectedCount(rejectedCount)
                .pendingCount(pendingCount)
                .build();
    }

    private OvertimeApplicationResponse toResponse(OvertimeApplication entity) {
        return OvertimeApplicationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .userName(entity.getUser().getName())
                .type(entity.getType().name())
                .overtimeDate(entity.getOvertimeDate())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .hours(entity.getHours())
                .reason(entity.getReason())
                .status(entity.getStatus().name())
                .approverId(entity.getApprover() != null ? entity.getApprover().getId() : null)
                .approverName(entity.getApprover() != null ? entity.getApprover().getName() : null)
                .approvedAt(entity.getApprovedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
