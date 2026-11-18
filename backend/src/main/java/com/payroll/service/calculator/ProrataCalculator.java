package com.payroll.service.calculator;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProrataCalculator {

    private static final int INTERMEDIATE_SCALE = 6;

    public record SalarySegment(LocalDate from, LocalDate to, int paidDays, BigDecimal monthlyAmount) {}

    public record SegmentTrace(
            LocalDate from,
            LocalDate to,
            int paidDays,
            int totalScheduledDays,
            BigDecimal factor,
            BigDecimal monthlyAmount,
            BigDecimal exactAmount
    ) {}

    public record ProrataResult(
            BigDecimal totalAmount,
            BigDecimal overallFactor,
            List<SegmentTrace> segments
    ) {}

    public ProrataResult calculateSingleSegment(LocalDate from, LocalDate to, int paidDays, int totalScheduledDays, BigDecimal monthlyAmount) {
        return calculateMultiSegment(List.of(new SalarySegment(from, to, paidDays, monthlyAmount)), totalScheduledDays);
    }

    public ProrataResult calculateMultiSegment(List<SalarySegment> segments, int totalScheduledDays) {
        if (totalScheduledDays <= 0 || segments == null || segments.isEmpty()) {
            return new ProrataResult(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal.ZERO.setScale(INTERMEDIATE_SCALE, RoundingMode.HALF_UP), List.of());
        }

        BigDecimal totalExact = BigDecimal.ZERO;
        int totalPaidDays = 0;
        List<SegmentTrace> traces = new ArrayList<>();

        for (SalarySegment seg : segments) {
            int safePaidDays = Math.max(0, Math.min(seg.paidDays(), totalScheduledDays));
            BigDecimal factor = BigDecimal.valueOf(safePaidDays)
                    .divide(BigDecimal.valueOf(totalScheduledDays), INTERMEDIATE_SCALE, RoundingMode.HALF_UP);
            BigDecimal exactAmount = seg.monthlyAmount()
                    .multiply(BigDecimal.valueOf(safePaidDays))
                    .divide(BigDecimal.valueOf(totalScheduledDays), INTERMEDIATE_SCALE, RoundingMode.HALF_UP);

            totalExact = totalExact.add(exactAmount);
            totalPaidDays += safePaidDays;

            traces.add(new SegmentTrace(
                    seg.from(),
                    seg.to(),
                    safePaidDays,
                    totalScheduledDays,
                    factor,
                    seg.monthlyAmount().setScale(2, RoundingMode.HALF_UP),
                    exactAmount
            ));
        }

        BigDecimal roundedTotal = totalExact.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
        BigDecimal overallFactor = BigDecimal.valueOf(totalPaidDays)
                .divide(BigDecimal.valueOf(totalScheduledDays), INTERMEDIATE_SCALE, RoundingMode.HALF_UP);

        return new ProrataResult(roundedTotal, overallFactor, traces);
    }
}
