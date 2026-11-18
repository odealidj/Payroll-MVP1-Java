package com.payroll.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record SimulatorOverrideRequest(
        @NotNull UUID payrollPeriodId,
        @NotNull UUID employmentId,
        String taxMethodOverride,
        BigDecimal bonusOverride,
        BigDecimal weekdayOvertimeHoursOverride,
        BigDecimal holidayOvertimeHoursOverride,
        BigDecimal unpaidLeaveDaysOverride
) {}
