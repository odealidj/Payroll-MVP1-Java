package com.payroll.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "companies")
public class Company {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "legal_name", nullable = false, length = 150)
    private String legalName;

    @Column(name = "npwp_masked", nullable = false, length = 30)
    private String npwpMasked;

    @Column(name = "jkk_risk_group", nullable = false)
    private Short jkkRiskGroup;

    @Column(name = "work_pattern", nullable = false, length = 10)
    private String workPattern;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public String getNpwpMasked() { return npwpMasked; }
    public void setNpwpMasked(String npwpMasked) { this.npwpMasked = npwpMasked; }
    public Short getJkkRiskGroup() { return jkkRiskGroup; }
    public void setJkkRiskGroup(Short jkkRiskGroup) { this.jkkRiskGroup = jkkRiskGroup; }
    public String getWorkPattern() { return workPattern; }
    public void setWorkPattern(String workPattern) { this.workPattern = workPattern; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
