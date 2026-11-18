package com.payroll.service.calculator;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class OvertimeCalculator {

    private static final BigDecimal DIVISOR_173 = BigDecimal.valueOf(173);
    private static final BigDecimal SEVENTY_FIVE_PCT = new BigDecimal("0.75");

    public record OvertimeTierBreakdown(String dayCategory, String label, BigDecimal hours, BigDecimal multiplier, BigDecimal amount) {}

    public record OvertimeResult(
            BigDecimal baseWageUsed,
            boolean seventyFivePercentRuleApplied,
            BigDecimal hourlyRate,
            BigDecimal weekdayAmount,
            BigDecimal holidayAmount,
            BigDecimal totalAmount,
            List<OvertimeTierBreakdown> breakdown
    ) {}

    public OvertimeResult calculate(
            BigDecimal fixedMonthlyWage,
            BigDecimal totalRegularWage,
            boolean overtimeEligible,
            BigDecimal weekdayHours,
            BigDecimal holidayHours
    ) {
        if (!overtimeEligible || fixedMonthlyWage.compareTo(BigDecimal.ZERO) <= 0) {
            return zeroResult();
        }
        BigDecimal safeWeekday = weekdayHours != null ? weekdayHours.max(BigDecimal.ZERO) : BigDecimal.ZERO;
        BigDecimal safeHoliday = holidayHours != null ? holidayHours.max(BigDecimal.ZERO) : BigDecimal.ZERO;

        if (safeWeekday.compareTo(BigDecimal.ZERO) == 0 && safeHoliday.compareTo(BigDecimal.ZERO) == 0) {
            return zeroResult();
        }

        boolean apply75Rule = false;
        BigDecimal baseWage = fixedMonthlyWage;
        if (totalRegularWage != null && totalRegularWage.compareTo(fixedMonthlyWage) > 0) {
            BigDecimal fixedRatio = fixedMonthlyWage.divide(totalRegularWage, 6, RoundingMode.HALF_UP);
            if (fixedRatio.compareTo(SEVENTY_FIVE_PCT) < 0) {
                apply75Rule = true;
                baseWage = totalRegularWage.multiply(SEVENTY_FIVE_PCT).setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal hourlyRate = baseWage.divide(DIVISOR_173, 4, RoundingMode.HALF_UP);
        List<OvertimeTierBreakdown> breakdown = new ArrayList<>();

        BigDecimal weekdayExact = BigDecimal.ZERO;
        if (safeWeekday.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal firstHour = safeWeekday.min(BigDecimal.ONE);
            BigDecimal firstAmt = firstHour.multiply(new BigDecimal("1.50")).multiply(hourlyRate);
            weekdayExact = weekdayExact.add(firstAmt);
            breakdown.add(new OvertimeTierBreakdown("WEEKDAY", "Jam ke-1 (1.5x)", firstHour, new BigDecimal("1.50"), firstAmt.setScale(2, RoundingMode.HALF_UP)));

            BigDecimal restHours = safeWeekday.subtract(BigDecimal.ONE).max(BigDecimal.ZERO);
            if (restHours.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal restAmt = restHours.multiply(new BigDecimal("2.00")).multiply(hourlyRate);
                weekdayExact = weekdayExact.add(restAmt);
                breakdown.add(new OvertimeTierBreakdown("WEEKDAY", "Jam ke-2+ (2.0x)", restHours, new BigDecimal("2.00"), restAmt.setScale(2, RoundingMode.HALF_UP)));
            }
        }

        BigDecimal holidayExact = BigDecimal.ZERO;
        if (safeHoliday.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal tier1 = safeHoliday.min(BigDecimal.valueOf(8));
            if (tier1.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal t1Amt = tier1.multiply(new BigDecimal("2.00")).multiply(hourlyRate);
                holidayExact = holidayExact.add(t1Amt);
                breakdown.add(new OvertimeTierBreakdown("HOLIDAY", "Jam ke-1 s/d 8 (2.0x)", tier1, new BigDecimal("2.00"), t1Amt.setScale(2, RoundingMode.HALF_UP)));
            }

            BigDecimal tier2 = safeHoliday.subtract(BigDecimal.valueOf(8)).max(BigDecimal.ZERO).min(BigDecimal.ONE);
            if (tier2.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal t2Amt = tier2.multiply(new BigDecimal("3.00")).multiply(hourlyRate);
                holidayExact = holidayExact.add(t2Amt);
                breakdown.add(new OvertimeTierBreakdown("HOLIDAY", "Jam ke-9 (3.0x)", tier2, new BigDecimal("3.00"), t2Amt.setScale(2, RoundingMode.HALF_UP)));
            }

            BigDecimal tier3 = safeHoliday.subtract(BigDecimal.valueOf(9)).max(BigDecimal.ZERO);
            if (tier3.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal t3Amt = tier3.multiply(new BigDecimal("4.00")).multiply(hourlyRate);
                holidayExact = holidayExact.add(t3Amt);
                breakdown.add(new OvertimeTierBreakdown("HOLIDAY", "Jam ke-10+ (4.0x)", tier3, new BigDecimal("4.00"), t3Amt.setScale(2, RoundingMode.HALF_UP)));
            }
        }

        BigDecimal weekdayRounded = weekdayExact.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
        BigDecimal holidayRounded = holidayExact.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalRounded = weekdayRounded.add(holidayRounded);

        return new OvertimeResult(
                baseWage.setScale(2, RoundingMode.HALF_UP),
                apply75Rule,
                hourlyRate,
                weekdayRounded,
                holidayRounded,
                totalRounded,
                breakdown
        );
    }

    private OvertimeResult zeroResult() {
        BigDecimal z = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return new OvertimeResult(z, false, BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP), z, z, z, List.of());
    }
}
