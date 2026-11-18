package com.payroll.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tax_ter_brackets")
public class TaxTerBracket {
    @Id
    private UUID id;

    @Column(name = "rule_set_version_id", nullable = false)
    private UUID ruleSetVersionId;

    @Column(name = "ter_category", nullable = false, length = 1, columnDefinition = "char(1)")
    private String terCategory;

    @Column(name = "income_min", nullable = false, precision = 18, scale = 2)
    private BigDecimal incomeMin;

    @Column(name = "income_max", precision = 18, scale = 2)
    private BigDecimal incomeMax;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal rate;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRuleSetVersionId() { return ruleSetVersionId; }
    public void setRuleSetVersionId(UUID ruleSetVersionId) { this.ruleSetVersionId = ruleSetVersionId; }
    public String getTerCategory() { return terCategory; }
    public void setTerCategory(String terCategory) { this.terCategory = terCategory; }
    public BigDecimal getIncomeMin() { return incomeMin; }
    public void setIncomeMin(BigDecimal incomeMin) { this.incomeMin = incomeMin; }
    public BigDecimal getIncomeMax() { return incomeMax; }
    public void setIncomeMax(BigDecimal incomeMax) { this.incomeMax = incomeMax; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
}
