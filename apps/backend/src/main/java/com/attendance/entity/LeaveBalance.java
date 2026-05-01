package com.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "leave_balances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "leave_type_id", "year"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal totalDays;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal usedDays = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal remainingDays;

    @Column(nullable = false)
    private Integer year;

    @PrePersist
    protected void onCreate() {
        if (remainingDays == null) {
            remainingDays = totalDays;
        }
    }
}
