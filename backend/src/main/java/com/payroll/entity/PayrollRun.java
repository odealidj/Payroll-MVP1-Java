package com.payroll.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payroll_runs")
public class PayrollRun {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "payroll_period_id", nullable = false)
    private UUID payrollPeriodId;

    @Column(name = "run_number", nullable = false, unique = true, length = 40)
    private String runNumber;

    @Column(name = "run_type", nullable = false, length = 25)
    private String runType;

    @Column(name = "rule_set_version_id", nullable = false)
    private UUID ruleSetVersionId;

    @Column(name = "company_policy_version_id", nullable = false)
    private UUID companyPolicyVersionId;

    @Column(name = "engine_version", nullable = false, length = 20)
    private String engineVersion;

    @Column(nullable = false, length = 25)
    private String status;

    @Column(name = "total_employees", nullable = false)
    private int totalEmployees;

    @Column(name = "total_gross", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalGross;

    @Column(name = "total_deductions", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalDeductions;

    @Column(name = "total_net_pay", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalNetPay;

    @Column(name = "total_employer_cost", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalEmployerCost;

    @Column(name = "snapshot_locked_at")
    private Instant snapshotLockedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getPayrollPeriodId() { return payrollPeriodId; }
    public void setPayrollPeriodId(UUID payrollPeriodId) { this.payrollPeriodId = payrollPeriodId; }
    public String getRunNumber() { return runNumber; }
    public void setRunNumber(String runNumber) { this.runNumber = runNumber; }
    public String getRunType() { return runType; }
    public void setRunType(String runType) { this.runType = runType; }
    public UUID getRuleSetVersionId() { return ruleSetVersionId; }
    public void setRuleSetVersionId(UUID ruleSetVersionId) { this.ruleSetVersionId = ruleSetVersionId; }
    public UUID getCompanyPolicyVersionId() { return companyPolicyVersionId; }
    public void setCompanyPolicyVersionId(UUID companyPolicyVersionId) { this.companyPolicyVersionId = companyPolicyVersionId; }
    public String getEngineVersion() { return engineVersion; }
    public void setEngineVersion(String engineVersion) { this.engineVersion = engineVersion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalEmployees() { return totalEmployees; }
    public void setTotalEmployees(int totalEmployees) { this.totalEmployees = totalEmployees; }
    public BigDecimal getTotalGross() { return totalGross; }
    public void setTotalGross(BigDecimal totalGross) { this.totalGross = totalGross; }
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
    public BigDecimal getTotalNetPay() { return totalNetPay; }
    public void setTotalNetPay(BigDecimal totalNetPay) { this.totalNetPay = totalNetPay; }
    public BigDecimal getTotalEmployerCost() { return totalEmployerCost; }
    public void setTotalEmployerCost(BigDecimal totalEmployerCost) { this.totalEmployerCost = totalEmployerCost; }
    public Instant getSnapshotLockedAt() { return snapshotLockedAt; }
    public void setSnapshotLockedAt(Instant snapshotLockedAt) { this.snapshotLockedAt = snapshotLockedAt; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
