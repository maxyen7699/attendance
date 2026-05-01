package com.attendance.repository;

import com.attendance.entity.OvertimeApplication;
import com.attendance.entity.OvertimeApplication.OvertimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface OvertimeApplicationRepository extends JpaRepository<OvertimeApplication, Long> {
    List<OvertimeApplication> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<OvertimeApplication> findByApproverIdAndStatus(Long approverId, OvertimeStatus status);
    List<OvertimeApplication> findByStatus(OvertimeStatus status);
    List<OvertimeApplication> findByOvertimeDateBetweenOrderByOvertimeDateDesc(LocalDate start, LocalDate end);
}
