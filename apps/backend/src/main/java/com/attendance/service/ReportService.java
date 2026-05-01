package com.attendance.service;

import com.attendance.dto.*;
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
public class ReportService {

    private final AttendanceRepository attendanceRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final OvertimeApplicationRepository overtimeApplicationRepository;
    private final UserRepository userRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(Long userId, Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        if (year == null) year = now.getYear();
        if (month == null) month = now.getMonthValue();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("使用者不存在"));

        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        // Calculate work days (exclude weekends)
        int totalWorkDays = calculateWorkDays(monthStart, monthEnd);

        // Query attendance records for the month
        LocalDateTime attStart = monthStart.atStartOfDay();
        LocalDateTime attEnd = monthEnd.atTime(LocalTime.MAX);
        List<AttendanceRecord> attendanceRecords = attendanceRepository
                .findByUserAndClockInBetweenOrderByClockInDesc(user, attStart, attEnd);

        int actualWorkDays = (int) attendanceRecords.stream()
                .map(r -> r.getClockIn().toLocalDate())
                .distinct()
                .count();

        int lateCount = (int) attendanceRecords.stream()
                .filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.LATE)
                .count();

        int earlyLeaveCount = (int) attendanceRecords.stream()
                .filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.EARLY_LEAVE)
                .count();

        // Query approved leave applications overlapping this month
        List<LeaveApplication> leaveApps = leaveApplicationRepository
                .findByUserIdAndDateRange(userId, monthStart, monthEnd);
        BigDecimal leaveDays = leaveApps.stream()
                .filter(la -> la.getStatus() == LeaveApplication.LeaveStatus.APPROVED)
                .map(la -> calculateLeaveDaysInMonth(la, monthStart, monthEnd))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Query approved overtime applications for this month
        List<OvertimeApplication> overtimeApps = overtimeApplicationRepository
                .findByUserIdAndOvertimeDateBetween(userId, monthStart, monthEnd);
        BigDecimal overtimeHours = overtimeApps.stream()
                .filter(oa -> oa.getStatus() == OvertimeApplication.OvertimeStatus.APPROVED)
                .map(OvertimeApplication::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Build daily records map: date string -> AttendanceRecord
        Map<String, AttendanceRecord> attByDate = attendanceRecords.stream()
                .collect(Collectors.toMap(
                        r -> r.getClockIn().toLocalDate().format(DATE_FMT),
                        r -> r,
                        (existing, replacement) -> existing // keep first if duplicates
                ));

        // Build leave map: date -> leave application (approved or other status)
        Map<String, LeaveApplication> leaveByDate = new LinkedHashMap<>();
        for (LeaveApplication la : leaveApps) {
            LocalDate d = la.getStartDate().isBefore(monthStart) ? monthStart : la.getStartDate();
            LocalDate end = la.getEndDate().isAfter(monthEnd) ? monthEnd : la.getEndDate();
            for (; !d.isAfter(end); d = d.plusDays(1)) {
                leaveByDate.put(d.format(DATE_FMT), la);
            }
        }

        // Build daily record list for each work day in the month
        List<MonthlyReportResponse.DailyRecord> dailyRecords = new ArrayList<>();
        for (LocalDate d = monthStart; !d.isAfter(monthEnd); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) continue;

            String dateStr = d.format(DATE_FMT);
            AttendanceRecord att = attByDate.get(dateStr);
            LeaveApplication leave = leaveByDate.get(dateStr);

            String clockIn = att != null ? att.getClockIn().toLocalTime().format(TIME_FMT) : null;
            String clockOut = att != null && att.getClockOut() != null
                    ? att.getClockOut().toLocalTime().format(TIME_FMT) : null;
            String attendanceStatus = att != null ? att.getStatus().name() : null;
            String leaveType = leave != null ? leave.getLeaveType().getName() : null;
            String leaveStatus = leave != null ? leave.getStatus().name() : null;

            dailyRecords.add(MonthlyReportResponse.DailyRecord.builder()
                    .date(dateStr)
                    .clockIn(clockIn)
                    .clockOut(clockOut)
                    .attendanceStatus(attendanceStatus)
                    .leaveType(leaveType)
                    .leaveStatus(leaveStatus)
                    .build());
        }

        return MonthlyReportResponse.builder()
                .year(year)
                .month(month)
                .userId(user.getId())
                .userName(user.getName())
                .totalWorkDays(totalWorkDays)
                .actualWorkDays(actualWorkDays)
                .lateCount(lateCount)
                .earlyLeaveCount(earlyLeaveCount)
                .leaveDays(leaveDays.setScale(1, RoundingMode.HALF_UP))
                .overtimeHours(overtimeHours.setScale(1, RoundingMode.HALF_UP))
                .dailyRecords(dailyRecords)
                .build();
    }

    @Transactional(readOnly = true)
    public List<DepartmentStatsResponse> getDepartmentStats(Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        if (year == null) year = now.getYear();
        if (month == null) month = now.getMonthValue();

        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        LocalDateTime attStart = monthStart.atStartOfDay();
        LocalDateTime attEnd = monthEnd.atTime(LocalTime.MAX);

        // Group active users by department
        List<User> activeUsers = userRepository.findByIsActiveTrue();
        Map<String, List<User>> deptUsers = activeUsers.stream()
                .filter(u -> u.getDepartment() != null)
                .collect(Collectors.groupingBy(User::getDepartment));

        List<DepartmentStatsResponse> result = new ArrayList<>();

        for (Map.Entry<String, List<User>> entry : deptUsers.entrySet()) {
            String dept = entry.getKey();
            List<User> users = entry.getValue();
            int totalEmployees = users.size();

            // Attendance records for all users in this department
            int totalPresentDays = 0;
            int totalPossibleDays = totalEmployees * calculateWorkDays(monthStart, monthEnd);
            int deptLateCount = 0;
            BigDecimal deptLeaveDays = BigDecimal.ZERO;
            BigDecimal deptOvertimeHours = BigDecimal.ZERO;

            for (User user : users) {
                List<AttendanceRecord> records = attendanceRepository
                        .findByUserAndClockInBetweenOrderByClockInDesc(user, attStart, attEnd);

                long distinctDays = records.stream()
                        .map(r -> r.getClockIn().toLocalDate())
                        .distinct()
                        .count();
                totalPresentDays += (int) distinctDays;

                deptLateCount += (int) records.stream()
                        .filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.LATE)
                        .count();

                // Leave days
                List<LeaveApplication> leaves = leaveApplicationRepository
                        .findByUserIdAndDateRange(user.getId(), monthStart, monthEnd);
                deptLeaveDays = deptLeaveDays.add(leaves.stream()
                        .filter(la -> la.getStatus() == LeaveApplication.LeaveStatus.APPROVED)
                        .map(la -> calculateLeaveDaysInMonth(la, monthStart, monthEnd))
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

                // Overtime hours
                List<OvertimeApplication> overtimes = overtimeApplicationRepository
                        .findByUserIdAndOvertimeDateBetween(user.getId(), monthStart, monthEnd);
                deptOvertimeHours = deptOvertimeHours.add(overtimes.stream()
                        .filter(oa -> oa.getStatus() == OvertimeApplication.OvertimeStatus.APPROVED)
                        .map(OvertimeApplication::getHours)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
            }

            double attendanceRate = totalPossibleDays > 0
                    ? (double) totalPresentDays / totalPossibleDays * 100.0
                    : 0.0;

            BigDecimal avgLeaveDays = totalEmployees > 0
                    ? deptLeaveDays.divide(BigDecimal.valueOf(totalEmployees), 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal avgOvertimeHours = totalEmployees > 0
                    ? deptOvertimeHours.divide(BigDecimal.valueOf(totalEmployees), 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            result.add(DepartmentStatsResponse.builder()
                    .department(dept)
                    .totalEmployees(totalEmployees)
                    .attendanceRate(Math.round(attendanceRate * 100.0) / 100.0)
                    .avgLeaveDays(avgLeaveDays)
                    .avgOvertimeHours(avgOvertimeHours)
                    .lateCount(deptLateCount)
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<LeaveStatsResponse> getLeaveStats(Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        if (year == null) year = now.getYear();
        if (month == null) month = now.getMonthValue();

        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        List<LeaveApplication> allLeaves = leaveApplicationRepository.findByDateRange(monthStart, monthEnd);

        // Group by leave type
        Map<Long, List<LeaveApplication>> byType = allLeaves.stream()
                .collect(Collectors.groupingBy(la -> la.getLeaveType().getId()));

        // Load all leave types for name lookup
        Map<Long, String> typeNames = leaveTypeRepository.findAll().stream()
                .collect(Collectors.toMap(LeaveType::getId, LeaveType::getName));

        List<LeaveStatsResponse> result = new ArrayList<>();

        for (Map.Entry<Long, List<LeaveApplication>> entry : byType.entrySet()) {
            List<LeaveApplication> apps = entry.getValue();
            String typeName = typeNames.getOrDefault(entry.getKey(), "未知");

            BigDecimal totalDays = apps.stream()
                    .map(la -> calculateLeaveDaysInMonth(la, monthStart, monthEnd))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int approvedCount = (int) apps.stream()
                    .filter(la -> la.getStatus() == LeaveApplication.LeaveStatus.APPROVED)
                    .count();
            int rejectedCount = (int) apps.stream()
                    .filter(la -> la.getStatus() == LeaveApplication.LeaveStatus.REJECTED)
                    .count();
            int pendingCount = (int) apps.stream()
                    .filter(la -> la.getStatus() == LeaveApplication.LeaveStatus.PENDING)
                    .count();

            result.add(LeaveStatsResponse.builder()
                    .leaveTypeName(typeName)
                    .applicationCount(apps.size())
                    .totalDays(totalDays.setScale(1, RoundingMode.HALF_UP))
                    .approvedCount(approvedCount)
                    .rejectedCount(rejectedCount)
                    .pendingCount(pendingCount)
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public OvertimeStatsResponse getOvertimeStats(Integer year, Integer month) {
        LocalDate now = LocalDate.now();
        if (year == null) year = now.getYear();
        if (month == null) month = now.getMonthValue();

        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        List<OvertimeApplication> apps = overtimeApplicationRepository
                .findByOvertimeDateBetweenOrderByOvertimeDateDesc(monthStart, monthEnd);

        BigDecimal totalHours = apps.stream()
                .map(OvertimeApplication::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long approvedCount = apps.stream()
                .filter(a -> a.getStatus() == OvertimeApplication.OvertimeStatus.APPROVED)
                .count();
        long rejectedCount = apps.stream()
                .filter(a -> a.getStatus() == OvertimeApplication.OvertimeStatus.REJECTED)
                .count();
        long pendingCount = apps.stream()
                .filter(a -> a.getStatus() == OvertimeApplication.OvertimeStatus.PENDING)
                .count();

        return OvertimeStatsResponse.builder()
                .totalHours(totalHours.setScale(1, RoundingMode.HALF_UP))
                .totalCount((long) apps.size())
                .approvedCount(approvedCount)
                .rejectedCount(rejectedCount)
                .pendingCount(pendingCount)
                .build();
    }

    /**
     * Count work days in a date range, excluding Saturdays and Sundays.
     */
    private int calculateWorkDays(LocalDate start, LocalDate end) {
        int count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return count;
    }

    /**
     * Calculate how many leave days fall within the given month range.
     * This accounts for leaves that span beyond the month boundaries.
     */
    private BigDecimal calculateLeaveDaysInMonth(LeaveApplication la, LocalDate monthStart, LocalDate monthEnd) {
        LocalDate effectiveStart = la.getStartDate().isBefore(monthStart) ? monthStart : la.getStartDate();
        LocalDate effectiveEnd = la.getEndDate().isAfter(monthEnd) ? monthEnd : la.getEndDate();

        if (effectiveStart.isAfter(effectiveEnd)) return BigDecimal.ZERO;

        long totalDaysInRange = effectiveStart.datesUntil(effectiveEnd.plusDays(1)).count();
        long workDaysInRange = effectiveStart.datesUntil(effectiveEnd.plusDays(1))
                .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY)
                .count();

        // Proportion: if leave is N total days with M work days, scale by work days in range
        long totalLeaveDays = la.getStartDate().datesUntil(la.getEndDate().plusDays(1)).count();
        if (totalLeaveDays == 0) return BigDecimal.ZERO;

        BigDecimal ratio = BigDecimal.valueOf(workDaysInRange)
                .divide(BigDecimal.valueOf(totalDaysInRange), 4, RoundingMode.HALF_UP);

        return la.getTotalDays().multiply(ratio).setScale(1, RoundingMode.HALF_UP);
    }
}
