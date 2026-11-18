package com.payroll.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PayrollResultSummaryDto(
        UUID id,
        UUID payrollRunId,
        UUID employmentId,
        String employeeNumber,
        String employeeName,
        String scenarioTitle,
        String contractType,
        String ptkpStatus,
        String terCategory,
        String taxMethod,
        BigDecimal grossEarnings,
        BigDecimal totalReimbursements,
        BigDecimal bpjsEmployeeTotal,
        BigDecimal bpjsEmployerTotal,
        BigDecimal pph21Amount,
        BigDecimal taxAllowance,
        BigDecimal loanDeduction,
        BigDecimal totalDeductions,
        BigDecimal netPay,
        BigDecimal totalTakeHomeTransfer,
        BigDecimal totalEmployerCost,
        String paymentStatus,
        List<String> anomalyFlags
) {}
