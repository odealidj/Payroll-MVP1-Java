package com.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record JournalDto(
        UUID id,
        UUID payrollRunId,
        String journalNumber,
        LocalDate postingDate,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        boolean balanced,
        String status,
        List<JournalLineDto> lines
) {
    public record JournalLineDto(
            int lineNo,
            String coaAccountCode,
            String accountName,
            String costCenterCode,
            BigDecimal debit,
            BigDecimal credit,
            String description
    ) {}
}
