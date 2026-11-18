package com.payroll.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payroll_periods")
public class PayrollPeriod {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "period_year", nullable = false)
    private Short periodYear;

    @Column(name = "period_month", nullable = false)
    private Short periodMonth;

    @Column(name = "period_name", nullable = false, length = 60)
    private String periodName;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "cutoff_date", nullable = false)
    private LocalDate cutoffDate;

    @Column(name = "pay_date", nullable = false)
    private LocalDate payDate;

    @Column(name = "scheduled_working_days", nullable = false)
    private int scheduledWorkingDays;

    @Column(name = "is_final_tax_period", nullable = false)
    private boolean finalTaxPeriod;

    @Column(nullable = false, length = 15)
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public Short getPeriodYear() { return periodYear; }
    public void setPeriodYear(Short periodYear) { this.periodYear = periodYear; }
    public Short getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(Short periodMonth) { this.periodMonth = periodMonth; }
    public String getPeriodName() { return periodName; }
    public void setPeriodName(String periodName) { this.periodName = periodName; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public LocalDate getCutoffDate() { return cutoffDate; }
    public void setCutoffDate(LocalDate cutoffDate) { this.cutoffDate = cutoffDate; }
    public LocalDate getPayDate() { return payDate; }
    public void setPayDate(LocalDate payDate) { this.payDate = payDate; }
    public int getScheduledWorkingDays() { return scheduledWorkingDays; }
    public void setScheduledWorkingDays(int scheduledWorkingDays) { this.scheduledWorkingDays = scheduledWorkingDays; }
    public boolean isFinalTaxPeriod() { return finalTaxPeriod; }
    public void setFinalTaxPeriod(boolean finalTaxPeriod) { this.finalTaxPeriod = finalTaxPeriod; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
