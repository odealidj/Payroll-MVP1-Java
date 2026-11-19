package com.payroll.integration;

import com.payroll.dto.*;
import com.payroll.entity.PayrollPeriod;
import com.payroll.repository.JournalRepository;
import com.payroll.repository.PayrollRunRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PayrollIntegrationTest {

    private static final UUID NOV_PERIOD_ID = UUID.fromString("40000000-0000-4000-8000-000000000011");
    private static final UUID DEC_PERIOD_ID = UUID.fromString("40000000-0000-4000-8000-000000000012");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PayrollRunRepository runRepository;

    @Autowired
    private JournalRepository journalRepository;

    private static UUID novRunId;
    private static UUID emp001EmploymentId;

    @Test
    @Order(1)
    void shouldLoadSeededMasterOverviewAndPeriods() {
        ResponseEntity<ApiResponse<MasterOverviewDto>> masterResp = restTemplate.exchange(
                "/api/v1/master/overview",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, masterResp.getStatusCode());
        assertNotNull(masterResp.getBody());
        assertTrue(masterResp.getBody().success());

        MasterOverviewDto master = masterResp.getBody().data();
        assertEquals("PT Nusantara Teknologi", master.company().legalName());
        assertEquals("PT-POL-2026-V1", master.activePolicy().versionCode());
        assertEquals("ID-REG-2026-V1", master.activeRuleSet().versionCode());
        assertEquals(125L, master.terBracketCount());
        assertEquals(10, master.employees().size());

        emp001EmploymentId = master.employees().stream()
                .filter(e -> "EMP-001".equals(e.employeeNumber()))
                .findFirst()
                .orElseThrow()
                .employmentId();

        ResponseEntity<ApiResponse<List<PayrollPeriod>>> periodsResp = restTemplate.exchange(
                "/api/v1/payroll-periods",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, periodsResp.getStatusCode());
        assertNotNull(periodsResp.getBody());
        assertEquals(2, periodsResp.getBody().data().size());
    }

    @Test
    @Order(2)
    void shouldCalculateNovemberRegularRunWithAllTenScenariosAndBalancedJournal() {
        ensurePeriodUnlockedForTest(NOV_PERIOD_ID);

        CalculateRunRequest req = new CalculateRunRequest(NOV_PERIOD_ID, "REGULAR", true);
        ResponseEntity<ApiResponse<PayrollRunDto>> calcResp = restTemplate.exchange(
                "/api/v1/payroll-runs/calculate",
                HttpMethod.POST,
                new HttpEntity<>(req),
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, calcResp.getStatusCode());
        PayrollRunDto run = ObjectsRequire(calcResp.getBody()).data();
        novRunId = run.id();
        assertEquals("REGULAR", run.runType());
        assertEquals("CALCULATED", run.status());
        assertEquals(10, run.totalEmployees());

        ResponseEntity<ApiResponse<List<PayrollResultSummaryDto>>> resultsResp = restTemplate.exchange(
                "/api/v1/payroll-runs/" + novRunId + "/results",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        List<PayrollResultSummaryDto> results = ObjectsRequire(resultsResp.getBody()).data();
        assertEquals(10, results.size());

        Map<String, PayrollResultSummaryDto> byNip = results.stream()
                .collect(java.util.stream.Collectors.toMap(PayrollResultSummaryDto::employeeNumber, r -> r));

        // Verify key scenarios
        assertEquals(0, new BigDecimal("261350.00").compareTo(byNip.get("EMP-001").pph21Amount()));
        assertEquals(0, new BigDecimal("590863.00").compareTo(byNip.get("EMP-002").bpjsEmployeeTotal()));
        assertEquals(0, byNip.get("EMP-003").taxAllowance().compareTo(byNip.get("EMP-003").pph21Amount()));
        assertEquals(0, new BigDecimal("9500000.00").compareTo(byNip.get("EMP-004").netPay()));
        assertEquals(0, new BigDecimal("5200000.00").compareTo(byNip.get("EMP-005").grossEarnings()));
        assertEquals(0, new BigDecimal("10547619.00").compareTo(byNip.get("EMP-006").grossEarnings()));
        assertEquals(0, new BigDecimal("10175000.00").compareTo(byNip.get("EMP-007").grossEarnings()));
        assertEquals(0, new BigDecimal("450000.00").compareTo(byNip.get("EMP-007").totalReimbursements()));
        assertEquals("ON_HOLD", byNip.get("EMP-008").paymentStatus());
        assertTrue(byNip.get("EMP-008").anomalyFlags().contains("DEDUCTION_OVER_50_PCT"));
        assertTrue(byNip.get("EMP-008").anomalyFlags().contains("UNVERIFIED_BANK_ACCOUNT"));
        assertEquals(0, new BigDecimal("14000000.00").compareTo(byNip.get("EMP-010").grossEarnings()));

        // Verify Calculation Trace JSON endpoint for EMP-006 (Multi-segment)
        ResponseEntity<ApiResponse<PayrollResultDetailDto>> traceResp = restTemplate.exchange(
                "/api/v1/payroll-runs/" + novRunId + "/results/" + byNip.get("EMP-006").id(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        PayrollResultDetailDto detail = ObjectsRequire(traceResp.getBody()).data();
        assertFalse(detail.items().isEmpty());
        assertNotNull(detail.calculationTrace().get("proration"));
        assertNotNull(detail.calculationTrace().get("bpjs"));
        assertNotNull(detail.calculationTrace().get("pph21"));

        // Verify Balanced Journal
        ResponseEntity<ApiResponse<JournalDto>> jrnResp = restTemplate.exchange(
                "/api/v1/payroll-runs/" + novRunId + "/journal",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        JournalDto journal = ObjectsRequire(jrnResp.getBody()).data();
        assertTrue(journal.balanced());
        assertEquals(0, journal.totalDebit().compareTo(journal.totalCredit()));
    }

    @Test
    @Order(3)
    void shouldHandleMultiRunThrInSameMonthWithoutDoubleCountingBpjs() {
        CalculateRunRequest thrReq = new CalculateRunRequest(NOV_PERIOD_ID, "THR", true);
        ResponseEntity<ApiResponse<PayrollRunDto>> thrResp = restTemplate.exchange(
                "/api/v1/payroll-runs/calculate",
                HttpMethod.POST,
                new HttpEntity<>(thrReq),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, thrResp.getStatusCode());
        PayrollRunDto thrRun = ObjectsRequire(thrResp.getBody()).data();

        try {
            assertEquals("THR", thrRun.runType());
            ResponseEntity<ApiResponse<List<PayrollResultSummaryDto>>> resultsResp = restTemplate.exchange(
                    "/api/v1/payroll-runs/" + thrRun.id() + "/results",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            List<PayrollResultSummaryDto> results = ObjectsRequire(resultsResp.getBody()).data();
            PayrollResultSummaryDto emp001Thr = results.stream()
                    .filter(r -> "EMP-001".equals(r.employeeNumber()))
                    .findFirst()
                    .orElseThrow();

            // BR-BPJS-TK-007: BPJS must NOT be deducted a second time in the same month
            assertEquals(0, BigDecimal.ZERO.compareTo(emp001Thr.bpjsEmployeeTotal()));
            assertEquals(0, BigDecimal.ZERO.compareTo(emp001Thr.bpjsEmployerTotal()));

            // BR-TAX-009: MTD combined gross = 10,454,000 (REG) + 10,000,000 (THR) = 20,454,000 (9% TER A = 1,840,860 - 261,350 = 1,579,510)
            assertEquals(0, new BigDecimal("1579510.00").compareTo(emp001Thr.pph21Amount()));
        } finally {
            // Clean up test THR run so live workspace stays in its default REGULAR state
            runRepository.deleteById(thrRun.id());
        }
    }

    @Test
    @Order(4)
    void shouldReconcileDecemberPasal17AndProduceTaxRefundForEmp009() {
        ensurePeriodUnlockedForTest(DEC_PERIOD_ID);

        CalculateRunRequest decReq = new CalculateRunRequest(DEC_PERIOD_ID, "REGULAR", true);
        ResponseEntity<ApiResponse<PayrollRunDto>> decResp = restTemplate.exchange(
                "/api/v1/payroll-runs/calculate",
                HttpMethod.POST,
                new HttpEntity<>(decReq),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, decResp.getStatusCode());
        PayrollRunDto decRun = ObjectsRequire(decResp.getBody()).data();
        assertTrue(decRun.finalTaxPeriod());
        // EMP-010 terminated on Nov 30 -> excluded from Dec run -> 9 employees
        assertEquals(9, decRun.totalEmployees());

        ResponseEntity<ApiResponse<List<PayrollResultSummaryDto>>> resultsResp = restTemplate.exchange(
                "/api/v1/payroll-runs/" + decRun.id() + "/results",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        List<PayrollResultSummaryDto> results = ObjectsRequire(resultsResp.getBody()).data();
        PayrollResultSummaryDto emp009 = results.stream()
                .filter(r -> "EMP-009".equals(r.employeeNumber()))
                .findFirst()
                .orElseThrow();

        assertEquals(0, new BigDecimal("-515200.00").compareTo(emp009.pph21Amount()));
        assertTrue(emp009.anomalyFlags().contains("TAX_REFUND_FINAL_PERIOD"));

        ResponseEntity<ApiResponse<JournalDto>> jrnResp = restTemplate.exchange(
                "/api/v1/payroll-runs/" + decRun.id() + "/journal",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        JournalDto decJournal = ObjectsRequire(jrnResp.getBody()).data();
        assertTrue(decJournal.balanced());
        assertEquals(0, decJournal.totalDebit().compareTo(decJournal.totalCredit()));
    }

    @Test
    @Order(5)
    void shouldApplyAndResetSimulatorOverrideCleanly() {
        SimulatorOverrideRequest simReq = new SimulatorOverrideRequest(
                NOV_PERIOD_ID,
                emp001EmploymentId,
                "GROSS_UP",
                new BigDecimal("5000000.00"),
                new BigDecimal("4.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        ResponseEntity<ApiResponse<PayrollRunDto>> simResp = restTemplate.exchange(
                "/api/v1/simulator/override",
                HttpMethod.POST,
                new HttpEntity<>(simReq),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, simResp.getStatusCode());
        PayrollRunDto simRun = ObjectsRequire(simResp.getBody()).data();
        novRunId = simRun.id();

        try {
            ResponseEntity<ApiResponse<List<PayrollResultSummaryDto>>> resResp = restTemplate.exchange(
                    "/api/v1/payroll-runs/" + simRun.id() + "/results",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            PayrollResultSummaryDto emp001Sim = ObjectsRequire(resResp.getBody()).data().stream()
                    .filter(r -> "EMP-001".equals(r.employeeNumber()))
                    .findFirst()
                    .orElseThrow();

            assertEquals("GROSS_UP", emp001Sim.taxMethod());
            assertTrue(emp001Sim.taxAllowance().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(emp001Sim.anomalyFlags().contains("SIMULATOR_OVERRIDE_ACTIVE"));
        } finally {
            ResponseEntity<ApiResponse<PayrollRunDto>> resetResp = restTemplate.exchange(
                    "/api/v1/simulator/reset?payrollPeriodId=" + NOV_PERIOD_ID,
                    HttpMethod.POST,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            assertEquals(HttpStatus.OK, resetResp.getStatusCode());
            novRunId = ObjectsRequire(resetResp.getBody()).data().id();
        }
    }

    @Test
    @Order(6)
    void shouldEnforceImmutabilityAfterRunApproval() {
        assertNotNull(novRunId);
        try {
            ResponseEntity<ApiResponse<PayrollRunDto>> approveResp = restTemplate.exchange(
                    "/api/v1/payroll-runs/" + novRunId + "/approve",
                    HttpMethod.POST,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            assertEquals(HttpStatus.OK, approveResp.getStatusCode());
            assertEquals("APPROVED", ObjectsRequire(approveResp.getBody()).data().status());

            // Subsequent approve or calculate REGULAR on same period must fail with 409 ERR_PAY_RUN_IMMUTABLE
            ResponseEntity<ApiResponse<Object>> reApproveResp = restTemplate.exchange(
                    "/api/v1/payroll-runs/" + novRunId + "/approve",
                    HttpMethod.POST,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            assertEquals(HttpStatus.CONFLICT, reApproveResp.getStatusCode());
            assertNotNull(ObjectsRequire(reApproveResp.getBody()).error());
            assertEquals("ERR_PAY_RUN_IMMUTABLE", ObjectsRequire(reApproveResp.getBody()).error().code());

            ResponseEntity<ApiResponse<Object>> recalcApprovedResp = restTemplate.exchange(
                    "/api/v1/payroll-runs/calculate",
                    HttpMethod.POST,
                    new HttpEntity<>(new CalculateRunRequest(NOV_PERIOD_ID, "REGULAR", true)),
                    new ParameterizedTypeReference<>() {}
            );
            assertEquals(HttpStatus.CONFLICT, recalcApprovedResp.getStatusCode());
            assertEquals("ERR_PAY_RUN_IMMUTABLE", ObjectsRequire(recalcApprovedResp.getBody()).error().code());
        } finally {
            // Restore run to CALCULATED so interactive simulator remains unlocked
            runRepository.findById(novRunId).ifPresent(r -> {
                r.setStatus("CALCULATED");
                r.setApprovedBy(null);
                r.setApprovedAt(null);
                runRepository.save(r);
            });
            journalRepository.findByPayrollRunId(novRunId).ifPresent(j -> {
                j.setStatus("DRAFT");
                journalRepository.save(j);
            });
        }
    }

    private void ensurePeriodUnlockedForTest(UUID periodId) {
        runRepository.findByPayrollPeriodIdOrderByCreatedAtAsc(periodId).stream()
                .filter(r -> "REGULAR".equals(r.getRunType()) && "APPROVED".equals(r.getStatus()))
                .forEach(r -> {
                    r.setStatus("CALCULATED");
                    r.setApprovedBy(null);
                    r.setApprovedAt(null);
                    runRepository.save(r);
                });
    }

    private <T> T ObjectsRequire(T value) {
        assertNotNull(value);
        return value;
    }
}
