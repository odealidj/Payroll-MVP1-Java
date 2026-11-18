package com.payroll.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "overtime_multiplier_rules")
public class OvertimeMultiplierRule {
    @Id
    private UUID id;

    @Column(name = "rule_set_version_id", nullable = false)
    private UUID ruleSetVersionId;

    @Column(name = "work_pattern", nullable = false, length = 10)
    private String workPattern;

    @Column(name = "day_category", nullable = false, length = 20)
    private String dayCategory;

    @Column(name = "hour_from", nullable = false)
    private Short hourFrom;

    @Column(name = "hour_to")
    private Short hourTo;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal multiplier;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getRuleSetVersionId() { return ruleSetVersionId; }
    public void setRuleSetVersionId(UUID ruleSetVersionId) { this.ruleSetVersionId = ruleSetVersionId; }
    public String getWorkPattern() { return workPattern; }
    public void setWorkPattern(String workPattern) { this.workPattern = workPattern; }
    public String getDayCategory() { return dayCategory; }
    public void setDayCategory(String dayCategory) { this.dayCategory = dayCategory; }
    public Short getHourFrom() { return hourFrom; }
    public void setHourFrom(Short hourFrom) { this.hourFrom = hourFrom; }
    public Short getHourTo() { return hourTo; }
    public void setHourTo(Short hourTo) { this.hourTo = hourTo; }
    public BigDecimal getMultiplier() { return multiplier; }
    public void setMultiplier(BigDecimal multiplier) { this.multiplier = multiplier; }
}
