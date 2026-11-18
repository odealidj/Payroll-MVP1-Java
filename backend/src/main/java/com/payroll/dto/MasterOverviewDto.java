package com.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MasterOverviewDto(
        CompanyInfo company,
        PolicyInfo activePolicy,
        RuleSetInfo activeRuleSet,
        List<SalaryComponentInfo> salaryComponents,
        List<BpjsConfigInfo> bpjsConfigs,
        List<Pasal17Info> pasal17Brackets,
        long terBracketCount,
        List<EmployeeScenarioInfo> employees
) {
    public record CompanyInfo(UUID id, String code, String legalName, String npwpMasked, Short jkkRiskGroup, String workPattern) {}
    public record PolicyInfo(String versionCode, String defaultProrateMethod, Short workingDayDivisor, BigDecimal varianceWarningPct, BigDecimal maxNonStatutoryDedPct) {}
    public record RuleSetInfo(UUID id, String versionCode, String regulationRef, LocalDate effectiveFrom) {}
    public record SalaryComponentInfo(String code, String name, String category, boolean isFixed, String taxTreatment, boolean bpjsKesBase, boolean overtimeBase, String coaAccountCode) {}
    public record BpjsConfigInfo(String programCode, Short riskGroup, BigDecimal employerRate, BigDecimal employeeRate, BigDecimal maxWageCap, Short maxAgeLimit) {}
    public record Pasal17Info(Short layerNo, BigDecimal pkpMin, BigDecimal pkpMax, BigDecimal rate) {}
    public record EmployeeScenarioInfo(
            UUID employmentId,
            String employeeNumber,
            String fullName,
            String scenarioTitle,
            String scenarioDescription,
            String contractType,
            LocalDate hireDate,
            LocalDate contractEndDate,
            LocalDate terminationDate,
            String ptkpStatus,
            String terCategory,
            String taxMethod,
            String bankCode,
            String bankAccountMasked,
            String bankVerificationStatus,
            String locationName,
            BigDecimal umpAmount,
            String costCenterCode,
            BigDecimal fixedMonthlyWage,
            int bpjsKesExtraDependents,
            boolean jkpEligible
    ) {}
}
