package com.attendance.service;

import com.attendance.dto.DashboardResponse;
import com.attendance.entity.*;
import com.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AttendanceRepository attendanceRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final OvertimeApplicationRepository overtimeApplicationRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        // Total active employees
        List<User> activeUsers = userRepository.findByIsActiveTrue();
        long totalEmployees = activeUsers.size();

        // Today attendance rate
        long todayClockedIn = 0;
        for (User user : activeUsers) {
            if (attendanceRepository.findByUserAndClockInBetween(user, todayStart, todayEnd).isPresent()) {
                todayClockedIn++;
            }
        }
        double todayAttendanceRate = totalEmployees > 0
                ? (double) todayClockedIn / totalEmployees * 100.0
                : 0.0;

        // Today late count
        List<AttendanceRecord> todayRecords = attendanceRepository
                .findByClockInBetweenOrderByClockInDesc(todayStart, todayEnd);
        int todayLateCount = (int) todayRecords.stream()
                .filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.LATE)
                .count();

        // Pending leave count
        int pendingLeaveCount = leaveApplicationRepository
                .findByStatus(LeaveApplication.LeaveStatus.PENDING).size();

        // Pending overtime count
        int pendingOvertimeCount = overtimeApplicationRepository
                .findByStatus(OvertimeApplication.OvertimeStatus.PENDING).size();

        // Monthly overtime hours (approved)
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        List<OvertimeApplication> monthlyOvertime = overtimeApplicationRepository
                .findByOvertimeDateBetweenOrderByOvertimeDateDesc(monthStart, monthEnd);
        BigDecimal monthlyOvertimeHours = monthlyOvertime.stream()
                .filter(oa -> oa.getStatus() == OvertimeApplication.OvertimeStatus.APPROVED)
                .map(OvertimeApplication::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(1, RoundingMode.HALF_UP);

        // Monthly leave days (approved)
        List<LeaveApplication> monthlyLeaves = leaveApplicationRepository
                .findByDateRange(monthStart, monthEnd);
        BigDecimal monthlyLeaveDays = monthlyLeaves.stream()
                .filter(la -> la.getStatus() == LeaveApplication.LeaveStatus.APPROVED)
                .map(LeaveApplication::getTotalDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(1, RoundingMode.HALF_UP);

        // Recent anomalies (last 7 days, max 10)
        LocalDate sevenDaysAgo = today.minusDays(7);
        LocalDateTime anomalyStart = sevenDaysAgo.atStartOfDay();
        List<AttendanceRecord> recentRecords = attendanceRepository
                .findByClockInBetweenOrderByClockInDesc(anomalyStart, todayEnd);

        List<DashboardResponse.RecentAnomaly> anomalies = recentRecords.stream()
                .filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.LATE
                        || r.getStatus() == AttendanceRecord.AttendanceStatus.EARLY_LEAVE
                        || r.getStatus() == AttendanceRecord.AttendanceStatus.ABSENT
                        || r.getStatus() == AttendanceRecord.AttendanceStatus.FORGOT)
                .sorted(Comparator.comparing(AttendanceRecord::getClockIn).reversed())
                .limit(10)
                .map(r -> DashboardResponse.RecentAnomaly.builder()
                        .userId(r.getUser().getId())
                        .userName(r.getUser().getName())
                        .date(r.getClockIn().toLocalDate().format(DATE_FMT))
                        .type(r.getStatus().name())
                        .detail(buildAnomalyDetail(r))
                        .build())
                .collect(Collectors.toList());

        // Also check for absent users in the last 7 work days
        // (users who should have records but don't)
        List<DashboardResponse.RecentAnomaly> absentAnomalies = findAbsentUsers(
                activeUsers, sevenDaysAgo, today, todayRecords);
        anomalies.addAll(absentAnomalies);

        // Sort all anomalies by date descending and limit to 10
        anomalies.sort(Comparator.comparing(DashboardResponse.RecentAnomaly::getDate).reversed());
        if (anomalies.size() > 10) {
            anomalies = anomalies.subList(0, 10);
        }

        return DashboardResponse.builder()
                .totalEmployees(totalEmployees)
                .todayAttendanceRate(Math.round(todayAttendanceRate * 100.0) / 100.0)
                .todayLateCount(todayLateCount)
                .pendingLeaveCount(pendingLeaveCount)
                .pendingOvertimeCount(pendingOvertimeCount)
                .monthlyOvertimeHours(monthlyOvertimeHours)
                .monthlyLeaveDays(monthlyLeaveDays)
                .recentAnomalies(anomalies)
                .build();
    }

    private String buildAnomalyDetail(AttendanceRecord record) {
        return switch (record.getStatus()) {
            case LATE -> "遲到 (上班: " + record.getClockIn().toLocalTime().format(
                    DateTimeFormatter.ofPattern("HH:mm")) + ")";
            case EARLY_LEAVE -> "早退 (下班: " + (record.getClockOut() != null
                    ? record.getClockOut().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                    : "未打卡") + ")";
            case ABSENT -> "曠職";
            case FORGOT -> "忘記打卡";
            default -> record.getStatus().name();
        };
    }

    /**
     * Find users who were absent (no attendance record on a work day) in the date range.
     */
    private List<DashboardResponse.RecentAnomaly> findAbsentUsers(
            List<User> activeUsers, LocalDate startDate, LocalDate endDate,
            List<AttendanceRecord> todayRecords) {

        List<DashboardResponse.RecentAnomaly> absences = new ArrayList<>();

        // Get all records in the range to determine who was present each day
        LocalDateTime rangeStart = startDate.atStartOfDay();
        LocalDateTime rangeEnd = endDate.atTime(LocalTime.MAX);
        List<AttendanceRecord> allRangeRecords = attendanceRepository
                .findByClockInBetweenOrderByClockInDesc(rangeStart, rangeEnd);

        // Group by date: date -> set of userIds who have records
        Map<LocalDate, Set<Long>> presentByDate = allRangeRecords.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getClockIn().toLocalDate(),
                        Collectors.mapping(r -> r.getUser().getId(), Collectors.toSet())
                ));

        // Check each work day for absent users
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) continue;
            if (d.equals(LocalDate.now())) continue; // skip today (not yet ended)

            Set<Long> presentUsers = presentByDate.getOrDefault(d, Collections.emptySet());
            for (User user : activeUsers) {
                if (!presentUsers.contains(user.getId())) {
                    absences.add(DashboardResponse.RecentAnomaly.builder()
                            .userId(user.getId())
                            .userName(user.getName())
                            .date(d.format(DATE_FMT))
                            .type("ABSENT")
                            .detail("未打卡（視為曠職）")
                            .build());
                }
            }
        }

        return absences;
    }
}
