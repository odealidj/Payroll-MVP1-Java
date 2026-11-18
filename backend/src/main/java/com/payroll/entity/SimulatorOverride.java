package com.payroll.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "simulator_overrides")
public class SimulatorOverride {
    @Id
    private UUID id;

    @Column(name = "payroll_period_id", nullable = false)
    private UUID payrollPeriodId;

    @Column(name = "employment_id", nullable = false)
    private UUID employmentId;

    @Column(name = "tax_method_override", length = 15)
    private String taxMethodOverride;

    @Column(name = "bonus_override", precision = 18, scale = 2)
    private BigDecimal bonusOverride;

    @Column(name = "weekday_overtime_hours_override", precision = 6, scale = 2)
    private BigDecimal weekdayOvertimeHoursOverride;

    @Column(name = "holiday_overtime_hours_override", precision = 6, scale = 2)
    private BigDecimal holidayOvertimeHoursOverride;

    @Column(name = "unpaid_leave_days_override", precision = 5, scale = 2)
    private BigDecimal unpaidLeaveDaysOverride;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPayrollPeriodId() { return payrollPeriodId; }
    public void setPayrollPeriodId(UUID payrollPeriodId) { this.payrollPeriodId = payrollPeriodId; }
    public UUID getEmploymentId() { return employmentId; }
    public void setEmploymentId(UUID employmentId) { this.employmentId = employmentId; }
    public String getTaxMethodOverride() { return taxMethodOverride; }
    public void setTaxMethodOverride(String taxMethodOverride) { this.taxMethodOverride = taxMethodOverride; }
    public BigDecimal getBonusOverride() { return bonusOverride; }
    public void setBonusOverride(BigDecimal bonusOverride) { this.bonusOverride = bonusOverride; }
    public BigDecimal getWeekdayOvertimeHoursOverride() { return weekdayOvertimeHoursOverride; }
    public void setWeekdayOvertimeHoursOverride(BigDecimal weekdayOvertimeHoursOverride) { this.weekdayOvertimeHoursOverride = weekdayOvertimeHoursOverride; }
    public BigDecimal getHolidayOvertimeHoursOverride() { return holidayOvertimeHoursOverride; }
    public void setHolidayOvertimeHoursOverride(BigDecimal holidayOvertimeHoursOverride) { this.holidayOvertimeHoursOverride = holidayOvertimeHoursOverride; }
    public BigDecimal getUnpaidLeaveDaysOverride() { return unpaidLeaveDaysOverride; }
    public void setUnpaidLeaveDaysOverride(BigDecimal unpaidLeaveDaysOverride) { this.unpaidLeaveDaysOverride = unpaidLeaveDaysOverride; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
