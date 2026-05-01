package com.attendance.repository;

import com.attendance.entity.LeaveApplication;
import com.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {
    List<LeaveApplication> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<LeaveApplication> findByStatus(LeaveApplication.LeaveStatus status);
    List<LeaveApplication> findByApproverIdAndStatus(Long approverId, LeaveApplication.LeaveStatus status);
    List<LeaveApplication> findByAgentIdAndStatus(Long agentId, LeaveApplication.LeaveStatus status);

    @Query("SELECT la FROM LeaveApplication la WHERE la.startDate <= :endDate AND la.endDate >= :startDate")
    List<LeaveApplication> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT la FROM LeaveApplication la WHERE la.user.id = :userId AND la.startDate <= :endDate AND la.endDate >= :startDate")
    List<LeaveApplication> findByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
