package com.attendance.repository;

import com.attendance.entity.AttendanceRecord;
import com.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findByUserAndClockInBetween(User user, LocalDateTime start, LocalDateTime end);
    List<AttendanceRecord> findByUserOrderByClockInDesc(User user);
    List<AttendanceRecord> findByClockInBetweenOrderByClockInDesc(LocalDateTime start, LocalDateTime end);
    List<AttendanceRecord> findByUserAndClockInBetweenOrderByClockInDesc(User user, LocalDateTime start, LocalDateTime end);
}
