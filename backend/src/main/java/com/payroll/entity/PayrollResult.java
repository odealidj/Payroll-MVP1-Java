package com.payroll.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payroll_results")
public class PayrollResult {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "payroll_run_id", nullable = false)
    private UUID payrollRunId;

    @Column(name = "employment_id", nullable = false)
    private UUID employmentId;

    @Column(name = "cost_center_id", nullable = false)
    private UUID costCenterId;

    @Column(name = "employee_number", nullable = false, length = 30)
    private String employeeNumber;

    @Column(name = "employee_name", nullable = false, length = 150)
    private String employeeName;

    @Column(name = "scenario_title", nullable = false, length = 150)
    private String scenarioTitle;

    @Column(name = "contract_type", nullable = false, length = 10)
    private String contractType;

    @Column(name = "ptkp_status", nullable = false, length = 10)
    private String ptkpStatus;

    @Column(name = "ter_category", nullable = false, length = 1, columnDefinition = "char(1)")
    private String terCategory;

    @Column(name = "tax_method", nullable = false, length = 15)
    private String taxMethod;

    @Column(name = "gross_earnings", nullable = false, precision = 18, scale = 2)
    private BigDecimal grossEarnings;

    @Column(name = "total_reimbursements", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalReimbursements;

    @Column(name = "bpjs_employee_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal bpjsEmployeeTotal;

    @Column(name = "bpjs_employer_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal bpjsEmployerTotal;

    @Column(name = "pph21_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal pph21Amount;

    @Column(name = "tax_allowance", nullable = false, precision = 18, scale = 2)
    private BigDecimal taxAllowance;

    @Column(name = "loan_deduction", nullable = false, precision = 18, scale = 2)
    private BigDecimal loanDeduction;

    @Column(name = "total_deductions", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalDeductions;

    @Column(name = "net_pay", nullable = false, precision = 18, scale = 2)
    private BigDecimal netPay;

    @Column(name = "total_take_home_transfer", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalTakeHomeTransfer;

    @Column(name = "total_employer_cost", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalEmployerCost;

    @Column(name = "payment_status", nullable = false, length = 25)
    private String paymentStatus;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "anomaly_flags_json", nullable = false, columnDefinition = "jsonb")
    private String anomalyFlagsJson;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "calculation_trace_json", nullable = false, columnDefinition = "jsonb")
    private String calculationTraceJson;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getPayrollRunId() { return payrollRunId; }
    public void setPayrollRunId(UUID payrollRunId) { this.payrollRunId = payrollRunId; }
    public UUID getEmploymentId() { return employmentId; }
    public void setEmploymentId(UUID employmentId) { this.employmentId = employmentId; }
    public UUID getCostCenterId() { return costCenterId; }
    public void setCostCenterId(UUID costCenterId) { this.costCenterId = costCenterId; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getScenarioTitle() { return scenarioTitle; }
    public void setScenarioTitle(String scenarioTitle) { this.scenarioTitle = scenarioTitle; }
    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }
    public String getPtkpStatus() { return ptkpStatus; }
    public void setPtkpStatus(String ptkpStatus) { this.ptkpStatus = ptkpStatus; }
    public String getTerCategory() { return terCategory; }
    public void setTerCategory(String terCategory) { this.terCategory = terCategory; }
    public String getTaxMethod() { return taxMethod; }
    public void setTaxMethod(String taxMethod) { this.taxMethod = taxMethod; }
    public BigDecimal getGrossEarnings() { return grossEarnings; }
    public void setGrossEarnings(BigDecimal grossEarnings) { this.grossEarnings = grossEarnings; }
    public BigDecimal getTotalReimbursements() { return totalReimbursements; }
    public void setTotalReimbursements(BigDecimal totalReimbursements) { this.totalReimbursements = totalReimbursements; }
    public BigDecimal getBpjsEmployeeTotal() { return bpjsEmployeeTotal; }
    public void setBpjsEmployeeTotal(BigDecimal bpjsEmployeeTotal) { this.bpjsEmployeeTotal = bpjsEmployeeTotal; }
    public BigDecimal getBpjsEmployerTotal() { return bpjsEmployerTotal; }
    public void setBpjsEmployerTotal(BigDecimal bpjsEmployerTotal) { this.bpjsEmployerTotal = bpjsEmployerTotal; }
    public BigDecimal getPph21Amount() { return pph21Amount; }
    public void setPph21Amount(BigDecimal pph21Amount) { this.pph21Amount = pph21Amount; }
    public BigDecimal getTaxAllowance() { return taxAllowance; }
    public void setTaxAllowance(BigDecimal taxAllowance) { this.taxAllowance = taxAllowance; }
    public BigDecimal getLoanDeduction() { return loanDeduction; }
    public void setLoanDeduction(BigDecimal loanDeduction) { this.loanDeduction = loanDeduction; }
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
    public BigDecimal getNetPay() { return netPay; }
    public void setNetPay(BigDecimal netPay) { this.netPay = netPay; }
    public BigDecimal getTotalTakeHomeTransfer() { return totalTakeHomeTransfer; }
    public void setTotalTakeHomeTransfer(BigDecimal totalTakeHomeTransfer) { this.totalTakeHomeTransfer = totalTakeHomeTransfer; }
    public BigDecimal getTotalEmployerCost() { return totalEmployerCost; }
    public void setTotalEmployerCost(BigDecimal totalEmployerCost) { this.totalEmployerCost = totalEmployerCost; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getAnomalyFlagsJson() { return anomalyFlagsJson; }
    public void setAnomalyFlagsJson(String anomalyFlagsJson) { this.anomalyFlagsJson = anomalyFlagsJson; }
    public String getCalculationTraceJson() { return calculationTraceJson; }
    public void setCalculationTraceJson(String calculationTraceJson) { this.calculationTraceJson = calculationTraceJson; }
}
