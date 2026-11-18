package com.payroll.controller;

import com.payroll.dto.*;
import com.payroll.entity.PayrollPeriod;
import com.payroll.entity.SimulatorOverride;
import com.payroll.service.PayrollEngineService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PayrollController {

    private final PayrollEngineService payrollEngineService;

    public PayrollController(PayrollEngineService payrollEngineService) {
        this.payrollEngineService = payrollEngineService;
    }

    @GetMapping("/health")
    public ApiResponse<String> health(@RequestHeader(value = "X-Correlation-Id", required = false) String corrId) {
        return ApiResponse.ok(resolveCorrId(corrId), "OK");
    }

    @GetMapping("/master/overview")
    public ApiResponse<MasterOverviewDto> getMasterOverview(@RequestHeader(value = "X-Correlation-Id", required = false) String corrId) {
        return ApiResponse.ok(resolveCorrId(corrId), payrollEngineService.getMasterOverview());
    }

    @GetMapping("/payroll-periods")
    public ApiResponse<List<PayrollPeriod>> getPeriods(@RequestHeader(value = "X-Correlation-Id", required = false) String corrId) {
        return ApiResponse.ok(resolveCorrId(corrId), payrollEngineService.getPeriods());
    }

    @GetMapping("/payroll-runs")
    public ApiResponse<List<PayrollRunDto>> getRuns(@RequestHeader(value = "X-Correlation-Id", required = false) String corrId) {
        return ApiResponse.ok(resolveCorrId(corrId), payrollEngineService.getRuns());
    }

    @PostMapping("/payroll-runs/calculate")
    public ApiResponse<PayrollRunDto> calculateRun(
            @RequestHeader(value = "X-Correlation-Id", required = false) String corrId,
            @Valid @RequestBody CalculateRunRequest request
    ) {
        return ApiResponse.ok(resolveCorrId(corrId), payrollEngineService.calculatePayrollRun(request));
    }

    @PostMapping("/payroll-runs/{runId}/approve")
    public ApiResponse<PayrollRunDto> approveRun(
            @RequestHeader(value = "X-Correlation-Id", required = false) String corrId,
            @PathVariable UUID runId
    ) {
        return ApiResponse.ok(resolveCorrId(corrId), payrollEngineService.approveRun(runId));
    }

    @GetMapping("/payroll-runs/{runId}/results")
    public ApiResponse<List<PayrollResultSummaryDto>> getRunResults(
            @RequestHeader(value = "X-Correlation-Id", required = false) String corrId,
            @PathVariable UUID runId
    ) {
        return ApiResponse.ok(resolveCorrId(corrId), payrollEngineService.getRunResults(runId));
    }

    @GetMapping("/payroll-runs/{runId}/results/{resultId}")
    public ApiResponse<PayrollResultDetailDto> getResultDetail(
            @RequestHeader(value = "X-Correlation-Id", required = false) String corrId,
            @PathVariable UUID runId,
            @PathVariable UUID resultId
    ) {
        return ApiResponse.ok(resolveCorrId(corrId), payrollEngineService.getResultDetail(runId, resultId));
    }

    @GetMapping("/payroll-runs/{runId}/journal")
    public ApiResponse<JournalDto> getRunJournal(
            @RequestHeader(value = "X-Correlation-Id", required = false) String corrId,
            @PathVariable UUID runId
    ) {
        return ApiResponse.ok(resolveCorrId(corrId), payrollEngineService.getRunJournal(runId));
    }

    @GetMapping("/simulator/overrides")
    public ApiResponse<List<SimulatorOverride>> getOverrides(
            @RequestHeader(value = "X-Correlation-Id", required = false) String corrId,
            @RequestParam UUID payrollPeriodId
    ) {
        return ApiResponse.ok(resolveCorrId(corrId), payrollEngineService.getOverridesForPeriod(payrollPeriodId));
    }

    @PostMapping("/simulator/override")
    public ApiResponse<PayrollRunDto> applySimulatorOverride(
            @RequestHeader(value = "X-Correlation-Id", required = false) String corrId,
            @Valid @RequestBody SimulatorOverrideRequest request
    ) {
        return ApiResponse.ok(resolveCorrId(corrId), payrollEngineService.applySimulatorOverrideAndRecalculate(request));
    }

    @PostMapping("/simulator/reset")
    public ApiResponse<PayrollRunDto> resetSimulator(
            @RequestHeader(value = "X-Correlation-Id", required = false) String corrId,
            @RequestParam UUID payrollPeriodId
    ) {
        return ApiResponse.ok(resolveCorrId(corrId), payrollEngineService.resetSimulatorAndRecalculate(payrollPeriodId));
    }

    private String resolveCorrId(String corrId) {
        return (corrId != null && !corrId.isBlank()) ? corrId : UUID.randomUUID().toString();
    }
}
