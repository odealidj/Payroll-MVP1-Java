package com.payroll.service.calculator;

import com.payroll.entity.BpjsProgramConfig;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class BpjsCalculator {

    public record BpjsResult(
            boolean singleMonthlyDeductionApplied,
            BigDecimal kesRawBase,
            BigDecimal kesCappedBase,
            BigDecimal kesEmployer,
            BigDecimal kesEmployee,
            BigDecimal kesExtraDependent,
            BigDecimal jhtBase,
            BigDecimal jhtEmployer,
            BigDecimal jhtEmployee,
            BigDecimal jkkEmployer,
            BigDecimal jkmEmployer,
            BigDecimal jpRawBase,
            BigDecimal jpCappedBase,
            BigDecimal jpCapLimit,
            BigDecimal jpEmployer,
            BigDecimal jpEmployee,
            boolean jkpEligible,
            BigDecimal totalEmployeeDeduction,
            BigDecimal totalEmployerCost,
            BigDecimal totalTaxableEmployerPremi
    ) {}

    public BpjsResult calculate(
            BigDecimal kesRawBase,
            BigDecimal tkRawBase,
            BigDecimal umpAmount,
            int extraDependents,
            int ageYears,
            String nationality,
            boolean jkpFlag,
            Map<String, BpjsProgramConfig> configs,
            boolean alreadyCalculatedThisMonth
    ) {
        BigDecimal zero = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        boolean isWni = "ID".equalsIgnoreCase(nationality);
        boolean isJkpEligible = jkpFlag && isWni && ageYears < 54;

        if (alreadyCalculatedThisMonth) {
            return new BpjsResult(
                    true, kesRawBase, zero, zero, zero, zero,
                    tkRawBase, zero, zero, zero, zero,
                    tkRawBase, zero, zero, zero, zero,
                    isJkpEligible, zero, zero, zero
            );
        }

        BpjsProgramConfig kesCfg = configs.get("BPJS_KES");
        BigDecimal kesCap = kesCfg != null && kesCfg.getMaxWageCap() != null ? kesCfg.getMaxWageCap() : new BigDecimal("12000000.00");
        BigDecimal kesFloor = umpAmount != null ? umpAmount : BigDecimal.ZERO;
        BigDecimal kesCapped = kesRawBase.max(kesFloor).min(kesCap).setScale(2, RoundingMode.HALF_UP);

        BigDecimal kesErRate = kesCfg != null ? kesCfg.getEmployerRate() : new BigDecimal("0.040000");
        BigDecimal kesEmpRate = kesCfg != null ? kesCfg.getEmployeeRate() : new BigDecimal("0.010000");

        BigDecimal kesEr = roundRupiah(kesCapped.multiply(kesErRate));
        BigDecimal kesEmp = roundRupiah(kesCapped.multiply(kesEmpRate));
        BigDecimal kesExtra = extraDependents > 0
                ? roundRupiah(kesCapped.multiply(new BigDecimal("0.010000")).multiply(BigDecimal.valueOf(extraDependents)))
                : zero;

        BpjsProgramConfig jhtCfg = configs.get("JHT");
        BigDecimal jhtEr = roundRupiah(tkRawBase.multiply(jhtCfg != null ? jhtCfg.getEmployerRate() : new BigDecimal("0.037000")));
        BigDecimal jhtEmp = roundRupiah(tkRawBase.multiply(jhtCfg != null ? jhtCfg.getEmployeeRate() : new BigDecimal("0.020000")));

        BpjsProgramConfig jkkCfg = configs.get("JKK");
        BigDecimal jkkEr = roundRupiah(tkRawBase.multiply(jkkCfg != null ? jkkCfg.getEmployerRate() : new BigDecimal("0.002400")));

        BpjsProgramConfig jkmCfg = configs.get("JKM");
        BigDecimal jkmEr = roundRupiah(tkRawBase.multiply(jkmCfg != null ? jkmCfg.getEmployerRate() : new BigDecimal("0.003000")));

        BpjsProgramConfig jpCfg = configs.get("JP");
        BigDecimal jpCap = jpCfg != null && jpCfg.getMaxWageCap() != null ? jpCfg.getMaxWageCap() : new BigDecimal("11086300.00");
        int jpMaxAge = jpCfg != null && jpCfg.getMaxAgeLimit() != null ? jpCfg.getMaxAgeLimit() : 58;
        BigDecimal jpCapped = tkRawBase.min(jpCap).setScale(2, RoundingMode.HALF_UP);

        BigDecimal jpEr = zero;
        BigDecimal jpEmp = zero;
        if (isWni && ageYears < jpMaxAge) {
            jpEr = roundRupiah(jpCapped.multiply(jpCfg != null ? jpCfg.getEmployerRate() : new BigDecimal("0.020000")));
            jpEmp = roundRupiah(jpCapped.multiply(jpCfg != null ? jpCfg.getEmployeeRate() : new BigDecimal("0.010000")));
        }

        BigDecimal empTotal = kesEmp.add(kesExtra).add(jhtEmp).add(jpEmp);
        BigDecimal erTotal = kesEr.add(jhtEr).add(jkkEr).add(jkmEr).add(jpEr);
        BigDecimal taxableErPremi = kesEr.add(jkkEr).add(jkmEr);

        return new BpjsResult(
                true,
                kesRawBase.setScale(2, RoundingMode.HALF_UP),
                kesCapped,
                kesEr,
                kesEmp,
                kesExtra,
                tkRawBase.setScale(2, RoundingMode.HALF_UP),
                jhtEr,
                jhtEmp,
                jkkEr,
                jkmEr,
                tkRawBase.setScale(2, RoundingMode.HALF_UP),
                jpCapped,
                jpCap,
                jpEr,
                jpEmp,
                isJkpEligible,
                empTotal,
                erTotal,
                taxableErPremi
        );
    }

    private BigDecimal roundRupiah(BigDecimal val) {
        return val.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
    }
}
