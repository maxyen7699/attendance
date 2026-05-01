package com.attendance.service;

import com.attendance.dto.*;
import com.attendance.entity.AttendanceRecord;
import com.attendance.entity.User;
import com.attendance.repository.AttendanceRepository;
import com.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    @Transactional
    public ClockInResponse clockIn(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        attendanceRepository.findByUserAndClockInBetween(user, todayStart, todayEnd)
                .ifPresent(record -> {
                    throw new RuntimeException("今天已經打卡");
                });

        LocalDateTime now = LocalDateTime.now();
        AttendanceRecord.AttendanceStatus status = AttendanceRecord.AttendanceStatus.NORMAL;

        // Late: clock-in at 09:00 or later (hour >= 9 and minute > 0 covers 09:01+, but spec says hour >= 9 && minute > 0)
        if (now.getHour() >= 9 && now.getMinute() > 0) {
            status = AttendanceRecord.AttendanceStatus.LATE;
        }

        AttendanceRecord record = AttendanceRecord.builder()
                .user(user)
                .clockIn(now)
                .status(status)
                .build();

        AttendanceRecord saved = attendanceRepository.save(record);

        return ClockInResponse.builder()
                .id(saved.getId())
                .userId(user.getId())
                .clockIn(saved.getClockIn())
                .status(saved.getStatus().name())
                .build();
    }

    @Transactional
    public ClockOutResponse clockOut(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        AttendanceRecord record = attendanceRepository.findByUserAndClockInBetween(user, todayStart, todayEnd)
                .orElseThrow(() -> new RuntimeException("尚未上班打卡"));

        if (record.getClockOut() != null) {
            throw new RuntimeException("已經下班打卡");
        }

        LocalDateTime now = LocalDateTime.now();
        record.setClockOut(now);

        // Re-evaluate status: if originally LATE, keep LATE; if NORMAL but clock-out before 18:00, set EARLY_LEAVE
        if (record.getStatus() == AttendanceRecord.AttendanceStatus.NORMAL && now.getHour() < 18) {
            record.setStatus(AttendanceRecord.AttendanceStatus.EARLY_LEAVE);
        }

        AttendanceRecord saved = attendanceRepository.save(record);

        return ClockOutResponse.builder()
                .id(saved.getId())
                .userId(user.getId())
                .clockIn(saved.getClockIn())
                .clockOut(saved.getClockOut())
                .status(saved.getStatus().name())
                .build();
    }

    @Transactional(readOnly = true)
    public TodayStatusResponse getTodayStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

        return attendanceRepository.findByUserAndClockInBetween(user, todayStart, todayEnd)
                .map(record -> TodayStatusResponse.builder()
                        .clockIn(record.getClockIn())
                        .clockOut(record.getClockOut())
                        .status(record.getStatus().name())
                        .hasClockedIn(true)
                        .hasClockedOut(record.getClockOut() != null)
                        .build())
                .orElse(TodayStatusResponse.builder()
                        .clockIn(null)
                        .clockOut(null)
                        .status(null)
                        .hasClockedIn(false)
                        .hasClockedOut(false)
                        .build());
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> getMyRecords(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return attendanceRepository.findByUserOrderByClockInDesc(user).stream()
                .map(this::toRecordResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> getAllRecords(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = (startDate != null ? startDate : LocalDate.of(2000, 1, 1)).atStartOfDay();
        LocalDateTime end = (endDate != null ? endDate : LocalDate.of(2099, 12, 31)).atTime(LocalTime.MAX);

        return attendanceRepository.findByClockInBetweenOrderByClockInDesc(start, end).stream()
                .map(this::toRecordResponse)
                .toList();
    }

    private AttendanceRecordResponse toRecordResponse(AttendanceRecord record) {
        return AttendanceRecordResponse.builder()
                .id(record.getId())
                .userId(record.getUser().getId())
                .userName(record.getUser().getName())
                .clockIn(record.getClockIn())
                .clockOut(record.getClockOut())
                .status(record.getStatus().name())
                .note(record.getNote())
                .build();
    }
}
