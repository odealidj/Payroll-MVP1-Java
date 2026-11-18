package com.payroll.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "rule_set_versions")
public class RuleSetVersion {
    @Id
    private UUID id;

    @Column(name = "jurisdiction_code", nullable = false, length = 10)
    private String jurisdictionCode;

    @Column(name = "version_code", nullable = false, unique = true, length = 30)
    private String versionCode;

    @Column(name = "regulation_ref", nullable = false)
    private String regulationRef;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false, length = 20)
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getJurisdictionCode() { return jurisdictionCode; }
    public void setJurisdictionCode(String jurisdictionCode) { this.jurisdictionCode = jurisdictionCode; }
    public String getVersionCode() { return versionCode; }
    public void setVersionCode(String versionCode) { this.versionCode = versionCode; }
    public String getRegulationRef() { return regulationRef; }
    public void setRegulationRef(String regulationRef) { this.regulationRef = regulationRef; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
