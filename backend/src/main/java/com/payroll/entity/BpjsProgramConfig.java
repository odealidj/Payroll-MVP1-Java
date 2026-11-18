package com.payroll.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "bpjs_program_configs")
public class BpjsProgramConfig {
    @Id
    private UUID id;

    @Column(name = "rule_set_version_id", nullable = false)
    private UUID ruleSetVersionId;

    @Column(name = "program_code", nullable = false, length = 20)
    private String programCode;

    @Column(name = "risk_group")
    private Short riskGroup;

    @Column(name = "employer_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal employerRate;

    @Column(name = "employee_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal employeeRate;

    @Column(name = "min_wage_cap", precision = 18, scale = 2)
    private BigDecimal minWageCap;

    @Column(name = "max_wage_cap", precision = 18, scale = 2)
    private BigDecimal maxWageCap;

    @Column(name = "max_age_limit")
    private Short maxAgeLimit;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRuleSetVersionId() { return ruleSetVersionId; }
    public void setRuleSetVersionId(UUID ruleSetVersionId) { this.ruleSetVersionId = ruleSetVersionId; }
    public String getProgramCode() { return programCode; }
    public void setProgramCode(String programCode) { this.programCode = programCode; }
    public Short getRiskGroup() { return riskGroup; }
    public void setRiskGroup(Short riskGroup) { this.riskGroup = riskGroup; }
    public BigDecimal getEmployerRate() { return employerRate; }
    public void setEmployerRate(BigDecimal employerRate) { this.employerRate = employerRate; }
    public BigDecimal getEmployeeRate() { return employeeRate; }
    public void setEmployeeRate(BigDecimal employeeRate) { this.employeeRate = employeeRate; }
    public BigDecimal getMinWageCap() { return minWageCap; }
    public void setMinWageCap(BigDecimal minWageCap) { this.minWageCap = minWageCap; }
    public BigDecimal getMaxWageCap() { return maxWageCap; }
    public void setMaxWageCap(BigDecimal maxWageCap) { this.maxWageCap = maxWageCap; }
    public Short getMaxAgeLimit() { return maxAgeLimit; }
    public void setMaxAgeLimit(Short maxAgeLimit) { this.maxAgeLimit = maxAgeLimit; }
}
