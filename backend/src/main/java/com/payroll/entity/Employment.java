package com.payroll.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employments")
public class Employment {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "employee_number", nullable = false, length = 30)
    private String employeeNumber;

    @Column(name = "scenario_title", nullable = false, length = 150)
    private String scenarioTitle;

    @Column(name = "scenario_description", nullable = false)
    private String scenarioDescription;

    @Column(name = "contract_type", nullable = false, length = 10)
    private String contractType;

    @Column(name = "probation_end_date")
    private LocalDate probationEndDate;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "termination_reason", length = 50)
    private String terminationReason;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "overtime_eligible", nullable = false)
    private boolean overtimeEligible;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "position_id", nullable = false)
    private UUID positionId;

    @Column(name = "work_location_id", nullable = false)
    private UUID workLocationId;

    @Column(name = "cost_center_id", nullable = false)
    private UUID costCenterId;

    @Column(name = "ptkp_status", nullable = false, length = 10)
    private String ptkpStatus;

    @Column(name = "ter_category", nullable = false, length = 1, columnDefinition = "char(1)")
    private String terCategory;

    @Column(name = "tax_method", nullable = false, length = 15)
    private String taxMethod;

    @Column(name = "bpjs_kes_extra_dependents", nullable = false)
    private int bpjsKesExtraDependents;

    @Column(name = "is_jkp_eligible", nullable = false)
    private boolean jkpEligible;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public String getScenarioTitle() { return scenarioTitle; }
    public void setScenarioTitle(String scenarioTitle) { this.scenarioTitle = scenarioTitle; }
    public String getScenarioDescription() { return scenarioDescription; }
    public void setScenarioDescription(String scenarioDescription) { this.scenarioDescription = scenarioDescription; }
    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }
    public LocalDate getProbationEndDate() { return probationEndDate; }
    public void setProbationEndDate(LocalDate probationEndDate) { this.probationEndDate = probationEndDate; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public LocalDate getContractEndDate() { return contractEndDate; }
    public void setContractEndDate(LocalDate contractEndDate) { this.contractEndDate = contractEndDate; }
    public LocalDate getTerminationDate() { return terminationDate; }
    public void setTerminationDate(LocalDate terminationDate) { this.terminationDate = terminationDate; }
    public String getTerminationReason() { return terminationReason; }
    public void setTerminationReason(String terminationReason) { this.terminationReason = terminationReason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isOvertimeEligible() { return overtimeEligible; }
    public void setOvertimeEligible(boolean overtimeEligible) { this.overtimeEligible = overtimeEligible; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }
    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
    public UUID getPositionId() { return positionId; }
    public void setPositionId(UUID positionId) { this.positionId = positionId; }
    public UUID getWorkLocationId() { return workLocationId; }
    public void setWorkLocationId(UUID workLocationId) { this.workLocationId = workLocationId; }
    public UUID getCostCenterId() { return costCenterId; }
    public void setCostCenterId(UUID costCenterId) { this.costCenterId = costCenterId; }
    public String getPtkpStatus() { return ptkpStatus; }
    public void setPtkpStatus(String ptkpStatus) { this.ptkpStatus = ptkpStatus; }
    public String getTerCategory() { return terCategory; }
    public void setTerCategory(String terCategory) { this.terCategory = terCategory; }
    public String getTaxMethod() { return taxMethod; }
    public void setTaxMethod(String taxMethod) { this.taxMethod = taxMethod; }
    public int getBpjsKesExtraDependents() { return bpjsKesExtraDependents; }
    public void setBpjsKesExtraDependents(int bpjsKesExtraDependents) { this.bpjsKesExtraDependents = bpjsKesExtraDependents; }
    public boolean isJkpEligible() { return jkpEligible; }
    public void setJkpEligible(boolean jkpEligible) { this.jkpEligible = jkpEligible; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
