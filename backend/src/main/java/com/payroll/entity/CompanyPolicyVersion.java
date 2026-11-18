package com.payroll.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "company_policy_versions")
public class CompanyPolicyVersion {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "version_code", nullable = false, unique = true, length = 30)
    private String versionCode;

    @Column(name = "default_prorate_method", nullable = false, length = 20)
    private String defaultProrateMethod;

    @Column(name = "working_day_divisor", nullable = false)
    private Short workingDayDivisor;

    @Column(name = "variance_warning_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal varianceWarningPct;

    @Column(name = "max_non_statutory_ded_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxNonStatutoryDedPct;

    @Column(name = "bank_change_risk_days", nullable = false)
    private Short bankChangeRiskDays;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false, length = 20)
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getVersionCode() { return versionCode; }
    public void setVersionCode(String versionCode) { this.versionCode = versionCode; }
    public String getDefaultProrateMethod() { return defaultProrateMethod; }
    public void setDefaultProrateMethod(String defaultProrateMethod) { this.defaultProrateMethod = defaultProrateMethod; }
    public Short getWorkingDayDivisor() { return workingDayDivisor; }
    public void setWorkingDayDivisor(Short workingDayDivisor) { this.workingDayDivisor = workingDayDivisor; }
    public BigDecimal getVarianceWarningPct() { return varianceWarningPct; }
    public void setVarianceWarningPct(BigDecimal varianceWarningPct) { this.varianceWarningPct = varianceWarningPct; }
    public BigDecimal getMaxNonStatutoryDedPct() { return maxNonStatutoryDedPct; }
    public void setMaxNonStatutoryDedPct(BigDecimal maxNonStatutoryDedPct) { this.maxNonStatutoryDedPct = maxNonStatutoryDedPct; }
    public Short getBankChangeRiskDays() { return bankChangeRiskDays; }
    public void setBankChangeRiskDays(Short bankChangeRiskDays) { this.bankChangeRiskDays = bankChangeRiskDays; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
