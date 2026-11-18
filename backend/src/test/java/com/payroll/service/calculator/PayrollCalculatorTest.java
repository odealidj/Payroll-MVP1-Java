package com.payroll.service.calculator;

import com.payroll.entity.BpjsProgramConfig;
import com.payroll.entity.TaxPasal17Bracket;
import com.payroll.entity.TaxTerBracket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PayrollCalculatorTest {

    private ProrataCalculator prorataCalculator;
    private OvertimeCalculator overtimeCalculator;
    private BpjsCalculator bpjsCalculator;
    private Pph21Calculator pph21Calculator;

    @BeforeEach
    void setUp() {
        prorataCalculator = new ProrataCalculator();
        overtimeCalculator = new OvertimeCalculator();
        bpjsCalculator = new BpjsCalculator();
        pph21Calculator = new Pph21Calculator();
    }

    @Test
    void shouldCalculateSingleSegmentAndMultiSegmentProrata() {
        ProrataCalculator.ProrataResult joiner = prorataCalculator.calculateSingleSegment(
                LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 30), 13, 21, new BigDecimal("8400000.00"));
        assertEquals(new BigDecimal("5200000.00"), joiner.totalAmount());

        ProrataCalculator.ProrataResult multiSeg = prorataCalculator.calculateMultiSegment(List.of(
                new ProrataCalculator.SalarySegment(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15), 11, new BigDecimal("8000000.00")),
                new ProrataCalculator.SalarySegment(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 30), 10, new BigDecimal("10000000.00"))
        ), 21);
        assertEquals(new BigDecimal("8952381.00"), multiSeg.totalAmount());
    }

    @Test
    void shouldCalculateOvertimePerPp352021() {
        OvertimeCalculator.OvertimeResult res = overtimeCalculator.calculate(
                new BigDecimal("8650000.00"),
                new BigDecimal("8650000.00"),
                true,
                new BigDecimal("4.00"),
                new BigDecimal("10.00")
        );
        assertEquals(new BigDecimal("50000.0000"), res.hourlyRate());
        assertEquals(new BigDecimal("375000.00"), res.weekdayAmount());
        assertEquals(new BigDecimal("1150000.00"), res.holidayAmount());
        assertEquals(new BigDecimal("1525000.00"), res.totalAmount());

        OvertimeCalculator.OvertimeResult ineligible = overtimeCalculator.calculate(
                new BigDecimal("18000000.00"), new BigDecimal("18000000.00"), false, new BigDecimal("5"), new BigDecimal("5"));
        assertEquals(new BigDecimal("0.00"), ineligible.totalAmount());
    }

    @Test
    void shouldCalculateBpjsCapsAndSingleMonthlyDeductionWithoutJkpDoubleCount() {
        Map<String, BpjsProgramConfig> cfgs = Map.of(
                "BPJS_KES", bpjsCfg("BPJS_KES", "0.040000", "0.010000", "12000000.00", null),
                "JHT", bpjsCfg("JHT", "0.037000", "0.020000", null, null),
                "JKK", bpjsCfg("JKK", "0.002400", "0.000000", null, null),
                "JKM", bpjsCfg("JKM", "0.003000", "0.000000", null, null),
                "JP", bpjsCfg("JP", "0.020000", "0.010000", "11086300.00", (short) 58)
        );

        BpjsCalculator.BpjsResult res = bpjsCalculator.calculate(
                new BigDecimal("18000000.00"),
                new BigDecimal("18000000.00"),
                new BigDecimal("5396761.00"),
                0,
                38,
                "ID",
                true,
                cfgs,
                false
        );

        assertEquals(new BigDecimal("12000000.00"), res.kesCappedBase());
        assertEquals(new BigDecimal("480000.00"), res.kesEmployer());
        assertEquals(new BigDecimal("120000.00"), res.kesEmployee());
        assertEquals(new BigDecimal("11086300.00"), res.jpCappedBase());
        assertEquals(new BigDecimal("221726.00"), res.jpEmployer());
        assertEquals(new BigDecimal("110863.00"), res.jpEmployee());
        assertEquals(new BigDecimal("666000.00"), res.jhtEmployer());
        assertEquals(new BigDecimal("360000.00"), res.jhtEmployee());
        assertEquals(new BigDecimal("43200.00"), res.jkkEmployer());
        assertEquals(new BigDecimal("54000.00"), res.jkmEmployer());
        assertTrue(res.jkpEligible());
        // Taxable employer premi = BPJS Kes 4% + JKK + JKM (NOT JHT 3.7% or JP 2%)
        assertEquals(new BigDecimal("577200.00"), res.totalTaxableEmployerPremi());

        // Second run in the same month (e.g. THR) must have 0 BPJS deductions
        BpjsCalculator.BpjsResult thrRun = bpjsCalculator.calculate(
                new BigDecimal("18000000.00"), new BigDecimal("18000000.00"),
                new BigDecimal("5396761.00"), 0, 38, "ID", true, cfgs, true);
        assertEquals(new BigDecimal("0.00"), thrRun.totalEmployeeDeduction());
        assertEquals(new BigDecimal("0.00"), thrRun.totalEmployerCost());
    }

    @Test
    void shouldCalculateTerMultiRunGrossUpAndPasal17Refund() {
        List<TaxTerBracket> terBrackets = List.of(
                ter("A", "0.00", "5400000.00", "0.000000"),
                ter("A", "10350000.01", "10700000.00", "0.025000"),
                ter("A", "19750000.01", "24150000.00", "0.090000"),
                ter("C", "14150000.01", "15550000.00", "0.050000"),
                ter("C", "15550000.01", "17050000.00", "0.060000")
        );

        // 1. Standard Monthly TER A
        Pph21Calculator.MonthlyTerResult m1 = pph21Calculator.calculateMonthlyTer(
                "A", new BigDecimal("10454000.00"), BigDecimal.ZERO, BigDecimal.ZERO, terBrackets);
        assertEquals(new BigDecimal("0.025000"), m1.terRateApplied());
        assertEquals(new BigDecimal("261350.00"), m1.currentTaxAmount());

        // 2. Multi-run MTD (BR-TAX-009): Regular 10,454,000 + THR 10,000,000 = 20,454,000 (9%)
        Pph21Calculator.MonthlyTerResult m2 = pph21Calculator.calculateMonthlyTer(
                "A", new BigDecimal("10000000.00"), new BigDecimal("10454000.00"), new BigDecimal("261350.00"), terBrackets);
        assertEquals(new BigDecimal("1840860.00"), m2.mtdTaxDue());
        assertEquals(new BigDecimal("1579510.00"), m2.currentTaxAmount());

        // 3. Gross-Up fixed-point convergence
        Pph21Calculator.GrossUpResult gu = pph21Calculator.solveMonthlyTerGrossUp(
                "C", new BigDecimal("14475600.00"), BigDecimal.ZERO, BigDecimal.ZERO, terBrackets);
        assertTrue(gu.taxAllowance().subtract(gu.pph21Amount()).abs().compareTo(BigDecimal.ONE) <= 0);
        assertTrue(gu.taxAllowance().compareTo(new BigDecimal("700000.00")) > 0);

        // 4. Pasal 17 Final Period with Tax Refund (Lebih Potong)
        List<TaxPasal17Bracket> p17Brackets = List.of(
                p17((short) 1, "0.00", "60000000.00", "0.050000"),
                p17((short) 2, "60000000.01", "250000000.00", "0.150000")
        );
        Pph21Calculator.Pasal17Result refund = pph21Calculator.calculatePasal17Final(
                "TK/0",
                new BigDecimal("132903200.00"),
                12,
                new BigDecimal("3240000.00"),
                new BigDecimal("4964650.00"),
                p17Brackets
        );
        assertTrue(refund.isRefund());
        assertTrue(refund.currentTaxAmount().compareTo(BigDecimal.ZERO) < 0);
    }

    private BpjsProgramConfig bpjsCfg(String code, String erRate, String empRate, String maxCap, Short maxAge) {
        BpjsProgramConfig c = new BpjsProgramConfig();
        c.setProgramCode(code);
        c.setEmployerRate(new BigDecimal(erRate));
        c.setEmployeeRate(new BigDecimal(empRate));
        if (maxCap != null) c.setMaxWageCap(new BigDecimal(maxCap));
        c.setMaxAgeLimit(maxAge);
        return c;
    }

    private TaxTerBracket ter(String cat, String min, String max, String rate) {
        TaxTerBracket b = new TaxTerBracket();
        b.setTerCategory(cat);
        b.setIncomeMin(new BigDecimal(min));
        b.setIncomeMax(max != null ? new BigDecimal(max) : null);
        b.setRate(new BigDecimal(rate));
        return b;
    }

    private TaxPasal17Bracket p17(short layer, String min, String max, String rate) {
        TaxPasal17Bracket b = new TaxPasal17Bracket();
        b.setLayerNo(layer);
        b.setPkpMin(new BigDecimal(min));
        b.setPkpMax(max != null ? new BigDecimal(max) : null);
        b.setRate(new BigDecimal(rate));
        return b;
    }
}
