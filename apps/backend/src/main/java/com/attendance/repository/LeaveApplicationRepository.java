package com.attendance.repository;

import com.attendance.entity.LeaveApplication;
import com.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {
    List<LeaveApplication> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<LeaveApplication> findByStatus(LeaveApplication.LeaveStatus status);
    List<LeaveApplication> findByApproverIdAndStatus(Long approverId, LeaveApplication.LeaveStatus status);
    List<LeaveApplication> findByAgentIdAndStatus(Long agentId, LeaveApplication.LeaveStatus status);
}
