package com.payroll.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PayrollResultDetailDto(
        PayrollResultSummaryDto summary,
        List<DetailItemDto> items,
        Map<String, Object> calculationTrace
) {
    public record DetailItemDto(
            UUID id,
            String componentCode,
            String componentName,
            String category,
            BigDecimal baseAmount,
            BigDecimal rate,
            BigDecimal prorateFactor,
            BigDecimal amount,
            String formulaDesc
    ) {}
}
