package com.payroll.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CalculateRunRequest(
        @NotNull UUID payrollPeriodId,
        String runType,
        Boolean lockSnapshot
) {}
