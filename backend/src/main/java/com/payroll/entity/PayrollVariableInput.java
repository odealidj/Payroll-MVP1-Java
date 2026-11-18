package com.payroll.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payroll_variable_inputs")
public class PayrollVariableInput {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "payroll_period_id", nullable = false)
    private UUID payrollPeriodId;

    @Column(name = "employment_id", nullable = false)
    private UUID employmentId;

    @Column(name = "salary_component_id", nullable = false)
    private UUID salaryComponentId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    private String notes;

    @Column(nullable = false, length = 20)
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getPayrollPeriodId() { return payrollPeriodId; }
    public void setPayrollPeriodId(UUID payrollPeriodId) { this.payrollPeriodId = payrollPeriodId; }
    public UUID getEmploymentId() { return employmentId; }
    public void setEmploymentId(UUID employmentId) { this.employmentId = employmentId; }
    public UUID getSalaryComponentId() { return salaryComponentId; }
    public void setSalaryComponentId(UUID salaryComponentId) { this.salaryComponentId = salaryComponentId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
