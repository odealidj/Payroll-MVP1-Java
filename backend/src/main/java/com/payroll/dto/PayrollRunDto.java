package com.payroll.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayrollRunDto(
        UUID id,
        UUID payrollPeriodId,
        String periodName,
        Short periodYear,
        Short periodMonth,
        boolean finalTaxPeriod,
        String runNumber,
        String runType,
        String status,
        String engineVersion,
        int totalEmployees,
        BigDecimal totalGross,
        BigDecimal totalDeductions,
        BigDecimal totalNetPay,
        BigDecimal totalEmployerCost,
        Instant snapshotLockedAt,
        String approvedBy,
        Instant approvedAt,
        Instant createdAt
) {}
