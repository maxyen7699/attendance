package com.attendance.service;

import com.attendance.dto.LeaveApplicationRequest;
import com.attendance.dto.LeaveApplicationResponse;
import com.attendance.dto.LeaveBalanceResponse;
import com.attendance.entity.*;
import com.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final UserRepository userRepository;

    @Transactional
    public LeaveApplicationResponse applyLeave(Long userId, LeaveApplicationRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("使用者不存在"));

        LeaveType leaveType = leaveTypeRepository.findById(req.getLeaveTypeId())
                .orElseThrow(() -> new RuntimeException("請假類型不存在"));

        int year = req.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceRepository.findByUserAndLeaveTypeAndYear(user, leaveType, year)
                .orElseThrow(() -> new RuntimeException("請假餘額不存在"));

        if (balance.getRemainingDays().compareTo(req.getTotalDays()) < 0) {
            throw new RuntimeException("請假額度不足");
        }

        User approver = user.getSupervisor();
        if (approver == null) {
            approver = user;
        }

        User agent = null;
        if (req.getAgentId() != null) {
            agent = userRepository.findById(req.getAgentId())
                    .orElseThrow(() -> new RuntimeException("代理人不存在"));
        }

        LeaveApplication application = LeaveApplication.builder()
                .user(user)
                .leaveType(leaveType)
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .totalDays(req.getTotalDays())
                .reason(req.getReason())
                .status(LeaveApplication.LeaveStatus.PENDING)
                .approver(approver)
                .agent(agent)
                .build();

        LeaveApplication saved = leaveApplicationRepository.save(application);
        return toApplicationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LeaveApplicationResponse> getMyLeaves(Long userId) {
        return leaveApplicationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toApplicationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveApplicationResponse> getPendingApprovals(Long userId) {
        List<LeaveApplication> results = new ArrayList<>();

        List<LeaveApplication> asApprover = leaveApplicationRepository
                .findByApproverIdAndStatus(userId, LeaveApplication.LeaveStatus.PENDING);
        results.addAll(asApprover);

        List<LeaveApplication> asAgent = leaveApplicationRepository
                .findByAgentIdAndStatus(userId, LeaveApplication.LeaveStatus.PENDING);
        // Avoid duplicates if the same person is both approver and agent
        for (LeaveApplication app : asAgent) {
            if (app.getApprover() == null || !app.getApprover().getId().equals(userId)) {
                results.add(app);
            }
        }

        return results.stream()
                .map(this::toApplicationResponse)
                .toList();
    }

    @Transactional
    public LeaveApplicationResponse approveLeave(Long applicationId, Long approverId) {
        LeaveApplication application = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("請假申請不存在"));

        if (application.getStatus() != LeaveApplication.LeaveStatus.PENDING) {
            throw new RuntimeException("此申請目前非待簽核狀態");
        }

        boolean isApprover = application.getApprover() != null
                && application.getApprover().getId().equals(approverId);
        boolean isAgent = application.getAgent() != null
                && application.getAgent().getId().equals(approverId);

        if (!isApprover && !isAgent) {
            throw new RuntimeException("您無權簽核此請假申請");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("簽核者不存在"));

        application.setStatus(LeaveApplication.LeaveStatus.APPROVED);
        application.setApprover(approver);
        application.setApprovedAt(LocalDateTime.now());

        // Deduct leave balance
        int year = application.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByUserAndLeaveTypeAndYear(application.getUser(), application.getLeaveType(), year)
                .orElseThrow(() -> new RuntimeException("請假餘額不存在"));

        balance.setUsedDays(balance.getUsedDays().add(application.getTotalDays()));
        balance.setRemainingDays(balance.getTotalDays().subtract(balance.getUsedDays()));
        leaveBalanceRepository.save(balance);

        LeaveApplication saved = leaveApplicationRepository.save(application);
        return toApplicationResponse(saved);
    }

    @Transactional
    public LeaveApplicationResponse rejectLeave(Long applicationId, Long approverId) {
        LeaveApplication application = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("請假申請不存在"));

        if (application.getStatus() != LeaveApplication.LeaveStatus.PENDING) {
            throw new RuntimeException("此申請目前非待簽核狀態");
        }

        boolean isApprover = application.getApprover() != null
                && application.getApprover().getId().equals(approverId);
        boolean isAgent = application.getAgent() != null
                && application.getAgent().getId().equals(approverId);

        if (!isApprover && !isAgent) {
            throw new RuntimeException("您無權簽核此請假申請");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("簽核者不存在"));

        application.setStatus(LeaveApplication.LeaveStatus.REJECTED);
        application.setApprover(approver);
        application.setApprovedAt(LocalDateTime.now());

        LeaveApplication saved = leaveApplicationRepository.save(application);
        return toApplicationResponse(saved);
    }

    @Transactional
    public LeaveApplicationResponse cancelLeave(Long applicationId, Long userId) {
        LeaveApplication application = leaveApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("請假申請不存在"));

        if (!application.getUser().getId().equals(userId)) {
            throw new RuntimeException("您無權取消此請假申請");
        }

        if (application.getStatus() != LeaveApplication.LeaveStatus.PENDING) {
            throw new RuntimeException("只有待簽核的申請可以取消");
        }

        application.setStatus(LeaveApplication.LeaveStatus.CANCELLED);

        LeaveApplication saved = leaveApplicationRepository.save(application);
        return toApplicationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getLeaveBalances(Long userId, Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        return leaveBalanceRepository.findByUserIdAndYear(userId, year).stream()
                .map(this::toBalanceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveApplicationResponse> getProxyTasks(Long userId) {
        return leaveApplicationRepository.findByAgentIdAndStatus(userId, LeaveApplication.LeaveStatus.PENDING)
                .stream()
                .map(this::toApplicationResponse)
                .toList();
    }

    private LeaveApplicationResponse toApplicationResponse(LeaveApplication entity) {
        return LeaveApplicationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .userName(entity.getUser().getName())
                .leaveTypeId(entity.getLeaveType().getId())
                .leaveTypeName(entity.getLeaveType().getName())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .totalDays(entity.getTotalDays())
                .reason(entity.getReason())
                .status(entity.getStatus().name())
                .approverId(entity.getApprover() != null ? entity.getApprover().getId() : null)
                .approverName(entity.getApprover() != null ? entity.getApprover().getName() : null)
                .agentId(entity.getAgent() != null ? entity.getAgent().getId() : null)
                .agentName(entity.getAgent() != null ? entity.getAgent().getName() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private LeaveBalanceResponse toBalanceResponse(LeaveBalance entity) {
        return LeaveBalanceResponse.builder()
                .leaveTypeId(entity.getLeaveType().getId())
                .leaveTypeName(entity.getLeaveType().getName())
                .totalDays(entity.getTotalDays())
                .usedDays(entity.getUsedDays())
                .remainingDays(entity.getRemainingDays())
                .year(entity.getYear())
                .build();
    }
}
