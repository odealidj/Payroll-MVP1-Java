package com.payroll.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "attendance_summaries")
public class AttendanceSummary {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "payroll_period_id", nullable = false)
    private UUID payrollPeriodId;

    @Column(name = "employment_id", nullable = false)
    private UUID employmentId;

    @Column(name = "scheduled_working_days", nullable = false)
    private int scheduledWorkingDays;

    @Column(name = "actual_working_days", nullable = false)
    private int actualWorkingDays;

    @Column(name = "paid_leave_days", nullable = false, precision = 5, scale = 2)
    private BigDecimal paidLeaveDays;

    @Column(name = "unpaid_leave_days", nullable = false, precision = 5, scale = 2)
    private BigDecimal unpaidLeaveDays;

    @Column(name = "absent_days", nullable = false, precision = 5, scale = 2)
    private BigDecimal absentDays;

    @Column(name = "weekday_overtime_hours", nullable = false, precision = 6, scale = 2)
    private BigDecimal weekdayOvertimeHours;

    @Column(name = "holiday_overtime_hours", nullable = false, precision = 6, scale = 2)
    private BigDecimal holidayOvertimeHours;

    @Column(name = "total_late_minutes", nullable = false)
    private int totalLateMinutes;

    @Column(nullable = false, length = 15)
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getPayrollPeriodId() { return payrollPeriodId; }
    public void setPayrollPeriodId(UUID payrollPeriodId) { this.payrollPeriodId = payrollPeriodId; }
    public UUID getEmploymentId() { return employmentId; }
    public void setEmploymentId(UUID employmentId) { this.employmentId = employmentId; }
    public int getScheduledWorkingDays() { return scheduledWorkingDays; }
    public void setScheduledWorkingDays(int scheduledWorkingDays) { this.scheduledWorkingDays = scheduledWorkingDays; }
    public int getActualWorkingDays() { return actualWorkingDays; }
    public void setActualWorkingDays(int actualWorkingDays) { this.actualWorkingDays = actualWorkingDays; }
    public BigDecimal getPaidLeaveDays() { return paidLeaveDays; }
    public void setPaidLeaveDays(BigDecimal paidLeaveDays) { this.paidLeaveDays = paidLeaveDays; }
    public BigDecimal getUnpaidLeaveDays() { return unpaidLeaveDays; }
    public void setUnpaidLeaveDays(BigDecimal unpaidLeaveDays) { this.unpaidLeaveDays = unpaidLeaveDays; }
    public BigDecimal getAbsentDays() { return absentDays; }
    public void setAbsentDays(BigDecimal absentDays) { this.absentDays = absentDays; }
    public BigDecimal getWeekdayOvertimeHours() { return weekdayOvertimeHours; }
    public void setWeekdayOvertimeHours(BigDecimal weekdayOvertimeHours) { this.weekdayOvertimeHours = weekdayOvertimeHours; }
    public BigDecimal getHolidayOvertimeHours() { return holidayOvertimeHours; }
    public void setHolidayOvertimeHours(BigDecimal holidayOvertimeHours) { this.holidayOvertimeHours = holidayOvertimeHours; }
    public int getTotalLateMinutes() { return totalLateMinutes; }
    public void setTotalLateMinutes(int totalLateMinutes) { this.totalLateMinutes = totalLateMinutes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
