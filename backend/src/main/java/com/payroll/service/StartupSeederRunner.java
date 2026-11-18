package com.payroll.service;

import com.payroll.dto.CalculateRunRequest;
import com.payroll.entity.PayrollPeriod;
import com.payroll.repository.PayrollRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StartupSeederRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupSeederRunner.class);

    private final PayrollRunRepository runRepository;
    private final PayrollEngineService engineService;

    public StartupSeederRunner(PayrollRunRepository runRepository, PayrollEngineService engineService) {
        this.runRepository = runRepository;
        this.engineService = engineService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedInitialRunsIfEmpty() {
        if (runRepository.count() > 0) {
            return;
        }
        try {
            List<PayrollPeriod> periods = engineService.getPeriods();
            for (PayrollPeriod period : periods) {
                engineService.calculatePayrollRun(new CalculateRunRequest(period.getId(), "REGULAR", true));
                log.info("Auto-calculated initial REGULAR payroll run for period: {}", period.getPeriodName());
            }
        } catch (Exception ex) {
            log.warn("Skipped initial auto-calculation on startup: {}", ex.getMessage());
        }
    }
}
