package com.payroll.service.calculator;

import com.payroll.entity.TaxPasal17Bracket;
import com.payroll.entity.TaxTerBracket;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class Pph21Calculator {

    private static final BigDecimal FIVE_PERCENT = new BigDecimal("0.05");
    private static final BigDecimal MAX_BIAYA_JABATAN_MONTH = new BigDecimal("500000.00");
    private static final BigDecimal MAX_BIAYA_JABATAN_YEAR = new BigDecimal("6000000.00");

    private static final Map<String, BigDecimal> PTKP_TABLE = Map.of(
            "TK/0", new BigDecimal("54000000.00"),
            "TK/1", new BigDecimal("58500000.00"),
            "K/0",  new BigDecimal("58500000.00"),
            "TK/2", new BigDecimal("63000000.00"),
            "K/1",  new BigDecimal("63000000.00"),
            "TK/3", new BigDecimal("67500000.00"),
            "K/2",  new BigDecimal("67500000.00"),
            "K/3",  new BigDecimal("72000000.00")
    );

    public record MonthlyTerResult(
            BigDecimal currentRunGrossTaxable,
            BigDecimal mtdPriorGross,
            BigDecimal mtdTotalGrossTaxable,
            BigDecimal terRateApplied,
            BigDecimal mtdTaxDue,
            BigDecimal mtdPriorTaxWithheld,
            BigDecimal currentTaxAmount
    ) {}

    public record LayerBreakdown(short layerNo, BigDecimal pkpSlice, BigDecimal rate, BigDecimal taxAmount) {}

    public record Pasal17Result(
            BigDecimal annualGrossTaxable,
            BigDecimal biayaJabatan,
            BigDecimal annualJhtJpWorker,
            BigDecimal annualNeto,
            BigDecimal ptkpAnnual,
            BigDecimal pkpAnnual,
            BigDecimal annualTaxDue,
            BigDecimal previouslyWithheld,
            BigDecimal currentTaxAmount,
            boolean isRefund,
            List<LayerBreakdown> layers
    ) {}

    public record GrossUpResult(
            BigDecimal taxAllowance,
            BigDecimal pph21Amount,
            int iterationsUsed,
            MonthlyTerResult terResult,
            Pasal17Result pasal17Result
    ) {}

    public BigDecimal getAnnualPtkp(String ptkpStatus) {
        return PTKP_TABLE.getOrDefault(ptkpStatus, new BigDecimal("54000000.00"));
    }

    public BigDecimal lookupTerRate(String terCategory, BigDecimal monthlyGrossTaxable, List<TaxTerBracket> brackets) {
        if (monthlyGrossTaxable.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return brackets.stream()
                .filter(b -> b.getTerCategory().equalsIgnoreCase(terCategory))
                .sorted(Comparator.comparing(TaxTerBracket::getIncomeMin))
                .filter(b -> b.getIncomeMax() == null || monthlyGrossTaxable.compareTo(b.getIncomeMax()) <= 0)
                .findFirst()
                .map(TaxTerBracket::getRate)
                .orElse(new BigDecimal("0.340000"));
    }

    public MonthlyTerResult calculateMonthlyTer(
            String terCategory,
            BigDecimal currentRunGrossTaxable,
            BigDecimal mtdPriorGross,
            BigDecimal mtdPriorTaxWithheld,
            List<TaxTerBracket> brackets
    ) {
        BigDecimal safeCurrent = currentRunGrossTaxable.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal safePriorGross = mtdPriorGross != null ? mtdPriorGross.max(BigDecimal.ZERO) : BigDecimal.ZERO;
        BigDecimal safePriorTax = mtdPriorTaxWithheld != null ? mtdPriorTaxWithheld.max(BigDecimal.ZERO) : BigDecimal.ZERO;

        BigDecimal mtdTotalGross = safePriorGross.add(safeCurrent).setScale(2, RoundingMode.HALF_UP);
        BigDecimal rate = lookupTerRate(terCategory, mtdTotalGross, brackets);
        BigDecimal mtdTaxDue = mtdTotalGross.multiply(rate).setScale(0, RoundingMode.FLOOR).setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentTax = mtdTaxDue.subtract(safePriorTax).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        return new MonthlyTerResult(
                safeCurrent,
                safePriorGross.setScale(2, RoundingMode.HALF_UP),
                mtdTotalGross,
                rate,
                mtdTaxDue,
                safePriorTax.setScale(2, RoundingMode.HALF_UP),
                currentTax
        );
    }

    public Pasal17Result calculatePasal17Final(
            String ptkpStatus,
            BigDecimal annualGrossTaxable,
            int activeMonthsInYear,
            BigDecimal annualJhtJpWorker,
            BigDecimal previouslyWithheld,
            List<TaxPasal17Bracket> brackets
    ) {
        BigDecimal safeAnnualGross = annualGrossTaxable.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        int months = Math.max(1, Math.min(12, activeMonthsInYear));
        BigDecimal maxBiayaJabatan = MAX_BIAYA_JABATAN_MONTH.multiply(BigDecimal.valueOf(months)).min(MAX_BIAYA_JABATAN_YEAR);
        BigDecimal biayaJabatan = safeAnnualGross.multiply(FIVE_PERCENT).setScale(0, RoundingMode.HALF_UP).min(maxBiayaJabatan).setScale(2, RoundingMode.HALF_UP);
        BigDecimal safePension = annualJhtJpWorker != null ? annualJhtJpWorker.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal neto = safeAnnualGross.subtract(biayaJabatan).subtract(safePension).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ptkp = getAnnualPtkp(ptkpStatus);
        BigDecimal rawPkp = neto.subtract(ptkp).max(BigDecimal.ZERO);
        BigDecimal pkp = rawPkp.divide(BigDecimal.valueOf(1000), 0, RoundingMode.FLOOR)
                .multiply(BigDecimal.valueOf(1000))
                .setScale(2, RoundingMode.HALF_UP);

        List<TaxPasal17Bracket> sorted = brackets.stream()
                .sorted(Comparator.comparing(TaxPasal17Bracket::getLayerNo))
                .toList();

        BigDecimal prevUpper = BigDecimal.ZERO;
        BigDecimal totalTaxExact = BigDecimal.ZERO;
        List<LayerBreakdown> layerBreakdowns = new ArrayList<>();

        for (TaxPasal17Bracket b : sorted) {
            if (pkp.compareTo(prevUpper) <= 0) {
                break;
            }
            BigDecimal upper = b.getPkpMax();
            BigDecimal slice = upper == null
                    ? pkp.subtract(prevUpper).max(BigDecimal.ZERO)
                    : pkp.min(upper).subtract(prevUpper).max(BigDecimal.ZERO);
            BigDecimal layerTax = slice.multiply(b.getRate());
            totalTaxExact = totalTaxExact.add(layerTax);
            layerBreakdowns.add(new LayerBreakdown(b.getLayerNo(), slice.setScale(2, RoundingMode.HALF_UP), b.getRate(), layerTax.setScale(2, RoundingMode.HALF_UP)));
            if (upper != null) {
                prevUpper = upper;
            }
        }

        BigDecimal annualTaxDue = totalTaxExact.setScale(0, RoundingMode.FLOOR).setScale(2, RoundingMode.HALF_UP);
        BigDecimal safePrevWithheld = previouslyWithheld != null ? previouslyWithheld.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentTax = annualTaxDue.subtract(safePrevWithheld).setScale(2, RoundingMode.HALF_UP);
        boolean isRefund = currentTax.compareTo(BigDecimal.ZERO) < 0;

        return new Pasal17Result(
                safeAnnualGross,
                biayaJabatan,
                safePension,
                neto,
                ptkp,
                pkp,
                annualTaxDue,
                safePrevWithheld,
                currentTax,
                isRefund,
                layerBreakdowns
        );
    }

    public GrossUpResult solveMonthlyTerGrossUp(
            String terCategory,
            BigDecimal baseGrossWithoutTaxAllowance,
            BigDecimal mtdPriorGross,
            BigDecimal mtdPriorTaxWithheld,
            List<TaxTerBracket> brackets
    ) {
        BigDecimal t = BigDecimal.ZERO;
        BigDecimal prevT = BigDecimal.valueOf(-10);
        MonthlyTerResult latest = null;
        int iter = 0;

        for (int i = 1; i <= 50; i++) {
            iter = i;
            latest = calculateMonthlyTer(terCategory, baseGrossWithoutTaxAllowance.add(t), mtdPriorGross, mtdPriorTaxWithheld, brackets);
            BigDecimal pph = latest.currentTaxAmount();
            if (pph.subtract(t).abs().compareTo(BigDecimal.ONE) <= 0) {
                // Converged: ensure T equals pph cleanly
                BigDecimal finalAllowance = pph;
                MonthlyTerResult finalCheck = calculateMonthlyTer(terCategory, baseGrossWithoutTaxAllowance.add(finalAllowance), mtdPriorGross, mtdPriorTaxWithheld, brackets);
                if (finalCheck.currentTaxAmount().compareTo(finalAllowance) == 0) {
                    return new GrossUpResult(finalAllowance, finalAllowance, iter, finalCheck, null);
                }
                return new GrossUpResult(t.setScale(2, RoundingMode.HALF_UP), pph, iter, latest, null);
            }
            if (pph.compareTo(prevT) == 0) {
                // Oscillation guard across bracket boundary: midpoint damping
                t = t.add(pph).divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
            } else {
                prevT = t;
                t = pph;
            }
        }
        return new GrossUpResult(t.setScale(2, RoundingMode.HALF_UP), latest != null ? latest.currentTaxAmount() : t, iter, latest, null);
    }

    public GrossUpResult solvePasal17GrossUp(
            String ptkpStatus,
            BigDecimal annualGrossWithoutCurrentTaxAllowance,
            int activeMonthsInYear,
            BigDecimal annualJhtJpWorker,
            BigDecimal previouslyWithheld,
            List<TaxPasal17Bracket> brackets
    ) {
        BigDecimal t = BigDecimal.ZERO;
        BigDecimal prevT = BigDecimal.valueOf(-10);
        Pasal17Result latest = null;
        int iter = 0;

        for (int i = 1; i <= 50; i++) {
            iter = i;
            latest = calculatePasal17Final(ptkpStatus, annualGrossWithoutCurrentTaxAllowance.add(t), activeMonthsInYear, annualJhtJpWorker, previouslyWithheld, brackets);
            BigDecimal pph = latest.currentTaxAmount().max(BigDecimal.ZERO);
            if (pph.subtract(t).abs().compareTo(BigDecimal.ONE) <= 0) {
                return new GrossUpResult(pph, latest.currentTaxAmount(), iter, null, latest);
            }
            if (pph.compareTo(prevT) == 0) {
                t = t.add(pph).divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
            } else {
                prevT = t;
                t = pph;
            }
        }
        return new GrossUpResult(t.setScale(2, RoundingMode.HALF_UP), latest != null ? latest.currentTaxAmount() : t, iter, null, latest);
    }
}
