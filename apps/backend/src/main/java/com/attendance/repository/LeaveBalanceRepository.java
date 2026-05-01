package com.attendance.repository;

import com.attendance.entity.LeaveBalance;
import com.attendance.entity.LeaveType;
import com.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    List<LeaveBalance> findByUserIdAndYear(Long userId, Integer year);
    Optional<LeaveBalance> findByUserAndLeaveTypeAndYear(User user, LeaveType leaveType, Integer year);
}
