package com.payroll.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payroll_details")
public class PayrollDetail {
    @Id
    private UUID id;

    @Column(name = "payroll_result_id", nullable = false)
    private UUID payrollResultId;

    @Column(name = "component_code", nullable = false, length = 30)
    private String componentCode;

    @Column(name = "component_name", nullable = false, length = 120)
    private String componentName;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "base_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal baseAmount;

    @Column(precision = 10, scale = 6)
    private BigDecimal rate;

    @Column(name = "prorate_factor", nullable = false, precision = 10, scale = 6)
    private BigDecimal prorateFactor;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "cost_center_id", nullable = false)
    private UUID costCenterId;

    @Column(name = "formula_desc", nullable = false)
    private String formulaDesc;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPayrollResultId() { return payrollResultId; }
    public void setPayrollResultId(UUID payrollResultId) { this.payrollResultId = payrollResultId; }
    public String getComponentCode() { return componentCode; }
    public void setComponentCode(String componentCode) { this.componentCode = componentCode; }
    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public BigDecimal getProrateFactor() { return prorateFactor; }
    public void setProrateFactor(BigDecimal prorateFactor) { this.prorateFactor = prorateFactor; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public UUID getCostCenterId() { return costCenterId; }
    public void setCostCenterId(UUID costCenterId) { this.costCenterId = costCenterId; }
    public String getFormulaDesc() { return formulaDesc; }
    public void setFormulaDesc(String formulaDesc) { this.formulaDesc = formulaDesc; }
}
