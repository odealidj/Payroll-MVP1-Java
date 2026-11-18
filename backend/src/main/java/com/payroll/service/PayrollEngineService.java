package com.payroll.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payroll.dto.*;
import com.payroll.entity.*;
import com.payroll.exception.DomainException;
import com.payroll.repository.*;
import com.payroll.service.calculator.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PayrollEngineService {

    private static final Logger log = LoggerFactory.getLogger(PayrollEngineService.class);
    private static final String ENGINE_VERSION = "1.1.0";

    private final CompanyRepository companyRepo;
    private final CompanyPolicyVersionRepository policyRepo;
    private final RuleSetVersionRepository ruleSetRepo;
    private final TaxTerBracketRepository terBracketRepo;
    private final TaxPasal17BracketRepository pasal17Repo;
    private final BpjsProgramConfigRepository bpjsConfigRepo;
    private final WorkLocationRepository workLocationRepo;
    private final CostCenterRepository costCenterRepo;
    private final EmployeeRepository employeeRepo;
    private final EmploymentRepository employmentRepo;
    private final SalaryComponentRepository componentRepo;
    private final EmployeeSalaryAssignmentRepository assignmentRepo;
    private final EmployeeYtdOpeningBalanceRepository ytdOpeningRepo;
    private final PayrollPeriodRepository periodRepo;
    private final AttendanceSummaryRepository attendanceRepo;
    private final PayrollVariableInputRepository variableInputRepo;
    private final EmployeeLoanRepository loanRepo;
    private final SimulatorOverrideRepository simulatorOverrideRepo;
    private final PayrollRunRepository runRepo;
    private final PayrollResultRepository resultRepo;
    private final PayrollDetailRepository detailRepo;
    private final JournalRepository journalRepo;
    private final JournalLineRepository journalLineRepo;

    private final ProrataCalculator prorataCalculator;
    private final OvertimeCalculator overtimeCalculator;
    private final BpjsCalculator bpjsCalculator;
    private final Pph21Calculator pph21Calculator;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public PayrollEngineService(
            CompanyRepository companyRepo,
            CompanyPolicyVersionRepository policyRepo,
            RuleSetVersionRepository ruleSetRepo,
            TaxTerBracketRepository terBracketRepo,
            TaxPasal17BracketRepository pasal17Repo,
            BpjsProgramConfigRepository bpjsConfigRepo,
            WorkLocationRepository workLocationRepo,
            CostCenterRepository costCenterRepo,
            EmployeeRepository employeeRepo,
            EmploymentRepository employmentRepo,
            SalaryComponentRepository componentRepo,
            EmployeeSalaryAssignmentRepository assignmentRepo,
            EmployeeYtdOpeningBalanceRepository ytdOpeningRepo,
            PayrollPeriodRepository periodRepo,
            AttendanceSummaryRepository attendanceRepo,
            PayrollVariableInputRepository variableInputRepo,
            EmployeeLoanRepository loanRepo,
            SimulatorOverrideRepository simulatorOverrideRepo,
            PayrollRunRepository runRepo,
            PayrollResultRepository resultRepo,
            PayrollDetailRepository detailRepo,
            JournalRepository journalRepo,
            JournalLineRepository journalLineRepo,
            ProrataCalculator prorataCalculator,
            OvertimeCalculator overtimeCalculator,
            BpjsCalculator bpjsCalculator,
            Pph21Calculator pph21Calculator,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate
    ) {
        this.companyRepo = companyRepo;
        this.policyRepo = policyRepo;
        this.ruleSetRepo = ruleSetRepo;
        this.terBracketRepo = terBracketRepo;
        this.pasal17Repo = pasal17Repo;
        this.bpjsConfigRepo = bpjsConfigRepo;
        this.workLocationRepo = workLocationRepo;
        this.costCenterRepo = costCenterRepo;
        this.employeeRepo = employeeRepo;
        this.employmentRepo = employmentRepo;
        this.componentRepo = componentRepo;
        this.assignmentRepo = assignmentRepo;
        this.ytdOpeningRepo = ytdOpeningRepo;
        this.periodRepo = periodRepo;
        this.attendanceRepo = attendanceRepo;
        this.variableInputRepo = variableInputRepo;
        this.loanRepo = loanRepo;
        this.simulatorOverrideRepo = simulatorOverrideRepo;
        this.runRepo = runRepo;
        this.resultRepo = resultRepo;
        this.detailRepo = detailRepo;
        this.journalRepo = journalRepo;
        this.journalLineRepo = journalLineRepo;
        this.prorataCalculator = prorataCalculator;
        this.overtimeCalculator = overtimeCalculator;
        this.bpjsCalculator = bpjsCalculator;
        this.pph21Calculator = pph21Calculator;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @Transactional(readOnly = true)
    public MasterOverviewDto getMasterOverview() {
        Company company = getActiveCompany();
        CompanyPolicyVersion policy = getActivePolicy(company.getId());
        RuleSetVersion ruleSet = getActiveRuleSet();

        List<SalaryComponent> components = componentRepo.findByCompanyIdOrderByCodeAsc(company.getId());
        List<BpjsProgramConfig> bpjsConfigs = bpjsConfigRepo.findByRuleSetVersionIdOrderByProgramCodeAsc(ruleSet.getId());
        List<TaxPasal17Bracket> pasal17 = pasal17Repo.findByRuleSetVersionIdOrderByLayerNoAsc(ruleSet.getId());
        List<TaxTerBracket> terBrackets = terBracketRepo.findByRuleSetVersionIdOrderByTerCategoryAscIncomeMinAsc(ruleSet.getId());

        Map<UUID, Employee> empMap = employeeRepo.findByCompanyId(company.getId()).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<UUID, WorkLocation> locMap = workLocationRepo.findByCompanyId(company.getId()).stream()
                .collect(Collectors.toMap(WorkLocation::getId, Function.identity()));
        Map<UUID, CostCenter> ccMap = costCenterRepo.findByCompanyId(company.getId()).stream()
                .collect(Collectors.toMap(CostCenter::getId, Function.identity()));
        Map<UUID, SalaryComponent> compById = components.stream()
                .collect(Collectors.toMap(SalaryComponent::getId, Function.identity()));

        LocalDate refDate = LocalDate.of(2026, 11, 30);
        List<MasterOverviewDto.EmployeeScenarioInfo> scenarios = employmentRepo.findByCompanyIdOrderByEmployeeNumberAsc(company.getId()).stream()
                .map(em -> {
                    Employee emp = empMap.get(em.getEmployeeId());
                    WorkLocation loc = locMap.get(em.getWorkLocationId());
                    CostCenter cc = ccMap.get(em.getCostCenterId());
                    BigDecimal fixedWage = assignmentRepo.findByEmploymentIdOrderByValidFromAsc(em.getId()).stream()
                            .filter(a -> !a.getValidFrom().isAfter(refDate) && (a.getValidTo() == null || !a.getValidTo().isBefore(refDate)))
                            .filter(a -> {
                                SalaryComponent sc = compById.get(a.getSalaryComponentId());
                                return sc != null && sc.isFixed();
                            })
                            .map(EmployeeSalaryAssignment::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new MasterOverviewDto.EmployeeScenarioInfo(
                            em.getId(),
                            em.getEmployeeNumber(),
                            emp.getFullName(),
                            em.getScenarioTitle(),
                            em.getScenarioDescription(),
                            em.getContractType(),
                            em.getHireDate(),
                            em.getContractEndDate(),
                            em.getTerminationDate(),
                            em.getPtkpStatus(),
                            em.getTerCategory(),
                            em.getTaxMethod(),
                            emp.getBankCode(),
                            emp.getBankAccountMasked(),
                            emp.getBankVerificationStatus(),
                            loc.getName(),
                            loc.getUmpAmount(),
                            cc.getCode(),
                            fixedWage.setScale(2, RoundingMode.HALF_UP),
                            em.getBpjsKesExtraDependents(),
                            em.isJkpEligible()
                    );
                }).toList();

        return new MasterOverviewDto(
                new MasterOverviewDto.CompanyInfo(company.getId(), company.getCode(), company.getLegalName(), company.getNpwpMasked(), company.getJkkRiskGroup(), company.getWorkPattern()),
                new MasterOverviewDto.PolicyInfo(policy.getVersionCode(), policy.getDefaultProrateMethod(), policy.getWorkingDayDivisor(), policy.getVarianceWarningPct(), policy.getMaxNonStatutoryDedPct()),
                new MasterOverviewDto.RuleSetInfo(ruleSet.getId(), ruleSet.getVersionCode(), ruleSet.getRegulationRef(), ruleSet.getEffectiveFrom()),
                components.stream().map(c -> new MasterOverviewDto.SalaryComponentInfo(c.getCode(), c.getName(), c.getCategory(), c.isFixed(), c.getTaxTreatment(), c.isBpjsKesBase(), c.isOvertimeBase(), c.getCoaAccountCode())).toList(),
                bpjsConfigs.stream().map(b -> new MasterOverviewDto.BpjsConfigInfo(b.getProgramCode(), b.getRiskGroup(), b.getEmployerRate(), b.getEmployeeRate(), b.getMaxWageCap(), b.getMaxAgeLimit())).toList(),
                pasal17.stream().map(p -> new MasterOverviewDto.Pasal17Info(p.getLayerNo(), p.getPkpMin(), p.getPkpMax(), p.getRate())).toList(),
                terBrackets.size(),
                scenarios
        );
    }

    @Transactional(readOnly = true)
    public List<PayrollPeriod> getPeriods() {
        Company company = getActiveCompany();
        return periodRepo.findByCompanyIdOrderByPeriodYearAscPeriodMonthAsc(company.getId());
    }

    @Transactional(readOnly = true)
    public List<PayrollRunDto> getRuns() {
        Company company = getActiveCompany();
        Map<UUID, PayrollPeriod> periodMap = periodRepo.findByCompanyIdOrderByPeriodYearAscPeriodMonthAsc(company.getId())
                .stream().collect(Collectors.toMap(PayrollPeriod::getId, Function.identity()));
        return runRepo.findByCompanyIdOrderByCreatedAtDesc(company.getId()).stream()
                .filter(r -> !"CANCELLED".equals(r.getStatus()))
                .map(r -> toRunDto(r, periodMap.get(r.getPayrollPeriodId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PayrollResultSummaryDto> getRunResults(UUID runId) {
        PayrollRun run = runRepo.findById(runId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "ERR_RUN_NOT_FOUND", "Payroll run tidak ditemukan."));
        return resultRepo.findByPayrollRunIdOrderByEmployeeNumberAsc(run.getId()).stream()
                .map(this::toResultSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public PayrollResultDetailDto getResultDetail(UUID runId, UUID resultId) {
        PayrollResult result = resultRepo.findByPayrollRunIdAndId(runId, resultId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "ERR_RESULT_NOT_FOUND", "Detail slip gaji tidak ditemukan."));
        List<PayrollResultDetailDto.DetailItemDto> items = detailRepo.findByPayrollResultId(result.getId()).stream()
                .map(d -> new PayrollResultDetailDto.DetailItemDto(
                        d.getId(), d.getComponentCode(), d.getComponentName(), d.getCategory(),
                        d.getBaseAmount(), d.getRate(), d.getProrateFactor(), d.getAmount(), d.getFormulaDesc()
                )).toList();
        return new PayrollResultDetailDto(toResultSummary(result), items, parseJsonMap(result.getCalculationTraceJson()));
    }

    @Transactional(readOnly = true)
    public JournalDto getRunJournal(UUID runId) {
        Journal journal = journalRepo.findByPayrollRunId(runId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "ERR_JOURNAL_NOT_FOUND", "Jurnal untuk payroll run ini belum tersedia."));
        List<JournalDto.JournalLineDto> lines = journalLineRepo.findByJournalIdOrderByLineNoAsc(journal.getId()).stream()
                .map(l -> new JournalDto.JournalLineDto(
                        l.getLineNo(), l.getCoaAccountCode(), l.getAccountName(), l.getCostCenterCode(), l.getDebit(), l.getCredit(), l.getDescription()
                )).toList();
        boolean balanced = journal.getTotalDebit().compareTo(journal.getTotalCredit()) == 0;
        return new JournalDto(journal.getId(), journal.getPayrollRunId(), journal.getJournalNumber(), journal.getPostingDate(),
                journal.getTotalDebit(), journal.getTotalCredit(), balanced, journal.getStatus(), lines);
    }

    @Transactional
    public PayrollRunDto approveRun(UUID runId) {
        PayrollRun run = runRepo.findById(runId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "ERR_RUN_NOT_FOUND", "Payroll run tidak ditemukan."));
        if ("APPROVED".equals(run.getStatus())) {
            throw new DomainException(HttpStatus.CONFLICT, "ERR_PAY_RUN_IMMUTABLE", "Payroll run telah berstatus APPROVED dan terkunci permanen.");
        }
        run.setStatus("APPROVED");
        run.setApprovedBy("Finance Manager & Director");
        run.setApprovedAt(Instant.now());
        runRepo.save(run);

        journalRepo.findByPayrollRunId(runId).ifPresent(j -> {
            j.setStatus("POSTED");
            journalRepo.save(j);
        });
        PayrollPeriod period = periodRepo.findById(run.getPayrollPeriodId()).orElse(null);
        return toRunDto(run, period);
    }

    @Transactional
    public List<SimulatorOverride> getOverridesForPeriod(UUID periodId) {
        return simulatorOverrideRepo.findByPayrollPeriodId(periodId);
    }

    @Transactional
    public PayrollRunDto applySimulatorOverrideAndRecalculate(SimulatorOverrideRequest req) {
        SimulatorOverride override = simulatorOverrideRepo.findByPayrollPeriodIdAndEmploymentId(req.payrollPeriodId(), req.employmentId())
                .orElseGet(() -> {
                    SimulatorOverride o = new SimulatorOverride();
                    o.setId(UUID.randomUUID());
                    o.setPayrollPeriodId(req.payrollPeriodId());
                    o.setEmploymentId(req.employmentId());
                    return o;
                });
        override.setTaxMethodOverride(req.taxMethodOverride());
        override.setBonusOverride(req.bonusOverride());
        override.setWeekdayOvertimeHoursOverride(req.weekdayOvertimeHoursOverride());
        override.setHolidayOvertimeHoursOverride(req.holidayOvertimeHoursOverride());
        override.setUnpaidLeaveDaysOverride(req.unpaidLeaveDaysOverride());
        override.setUpdatedAt(Instant.now());
        simulatorOverrideRepo.save(override);

        return calculatePayrollRun(new CalculateRunRequest(req.payrollPeriodId(), "REGULAR", true));
    }

    @Transactional
    public PayrollRunDto resetSimulatorAndRecalculate(UUID periodId) {
        List<SimulatorOverride> existing = simulatorOverrideRepo.findByPayrollPeriodId(periodId);
        if (!existing.isEmpty()) {
            simulatorOverrideRepo.deleteAll(existing);
        }
        return calculatePayrollRun(new CalculateRunRequest(periodId, "REGULAR", true));
    }

    @Transactional
    public PayrollRunDto calculatePayrollRun(CalculateRunRequest req) {
        Company company = getActiveCompany();
        CompanyPolicyVersion policy = getActivePolicy(company.getId());
        RuleSetVersion ruleSet = getActiveRuleSet();
        PayrollPeriod period = periodRepo.findById(req.payrollPeriodId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "ERR_PERIOD_NOT_FOUND", "Periode payroll tidak ditemukan."));

        String runType = (req.runType() == null || req.runType().isBlank()) ? "REGULAR" : req.runType().toUpperCase();

        // Acquire lightweight idempotency / status entry in Redis
        String redisLockKey = "payroll:calc:lock:" + period.getId() + ":" + runType;
        try {
            redisTemplate.opsForValue().set(redisLockKey, "RUNNING", Duration.ofSeconds(15));
        } catch (Exception e) {
            log.debug("Redis unavailable during lock write, continuing with DB transaction: {}", e.getMessage());
        }

        // Handle existing runs in this period
        List<PayrollRun> periodRuns = runRepo.findByPayrollPeriodIdOrderByCreatedAtAsc(period.getId());
        for (PayrollRun existing : periodRuns) {
            if (existing.getRunType().equals(runType) && !"CANCELLED".equals(existing.getStatus())) {
                if ("APPROVED".equals(existing.getStatus())) {
                    throw new DomainException(HttpStatus.CONFLICT, "ERR_PAY_RUN_IMMUTABLE",
                            "Payroll run " + runType + " untuk periode ini sudah berstatus APPROVED dan terkunci. Gunakan run tipe THR atau OFF_CYCLE.");
                }
                existing.setStatus("CANCELLED");
                runRepo.save(existing);
            }
        }

        // Prior active runs of OTHER types in the SAME month (for Multi-Run BR-TAX-009 & Single Monthly BPJS BR-BPJS-TK-007)
        List<PayrollRun> otherRunsSameMonth = periodRuns.stream()
                .filter(r -> !"CANCELLED".equals(r.getStatus()) && !r.getRunType().equals(runType))
                .toList();

        List<AttendanceSummary> periodAttendances = attendanceRepo.findByPayrollPeriodId(period.getId());
        if ("REGULAR".equals(runType) && periodAttendances.stream().anyMatch(a -> !"LOCKED".equals(a.getStatus()))) {
            throw new DomainException(HttpStatus.UNPROCESSABLE_ENTITY, "ERR_PAY_ATTENDANCE_NOT_LOCKED",
                    "Ringkasan absensi periode ini belum berstatus LOCKED.");
        }

        Map<UUID, AttendanceSummary> attByEmp = periodAttendances.stream()
                .collect(Collectors.toMap(AttendanceSummary::getEmploymentId, Function.identity()));
        Map<UUID, Employee> employeeMap = employeeRepo.findByCompanyId(company.getId()).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<UUID, WorkLocation> locMap = workLocationRepo.findByCompanyId(company.getId()).stream()
                .collect(Collectors.toMap(WorkLocation::getId, Function.identity()));
        Map<UUID, CostCenter> ccMap = costCenterRepo.findByCompanyId(company.getId()).stream()
                .collect(Collectors.toMap(CostCenter::getId, Function.identity()));
        Map<UUID, SalaryComponent> compById = componentRepo.findByCompanyIdOrderByCodeAsc(company.getId()).stream()
                .collect(Collectors.toMap(SalaryComponent::getId, Function.identity()));
        Map<String, SalaryComponent> compByCode = compById.values().stream()
                .collect(Collectors.toMap(SalaryComponent::getCode, Function.identity()));

        List<TaxTerBracket> terBrackets = terBracketRepo.findByRuleSetVersionIdOrderByTerCategoryAscIncomeMinAsc(ruleSet.getId());
        List<TaxPasal17Bracket> pasal17Brackets = pasal17Repo.findByRuleSetVersionIdOrderByLayerNoAsc(ruleSet.getId());
        Map<String, BpjsProgramConfig> bpjsConfigs = bpjsConfigRepo.findByRuleSetVersionIdOrderByProgramCodeAsc(ruleSet.getId())
                .stream().collect(Collectors.toMap(BpjsProgramConfig::getProgramCode, Function.identity(), (a, b) -> a));

        Map<UUID, List<PayrollVariableInput>> varsByEmp = variableInputRepo.findByPayrollPeriodId(period.getId()).stream()
                .collect(Collectors.groupingBy(PayrollVariableInput::getEmploymentId));
        Map<UUID, EmployeeLoan> loanByEmp = loanRepo.findByCompanyIdAndStatus(company.getId(), "REPAYING").stream()
                .collect(Collectors.toMap(EmployeeLoan::getEmploymentId, Function.identity(), (a, b) -> a));
        Map<UUID, SimulatorOverride> overrideByEmp = simulatorOverrideRepo.findByPayrollPeriodId(period.getId()).stream()
                .collect(Collectors.toMap(SimulatorOverride::getEmploymentId, Function.identity()));
        Map<UUID, EmployeeYtdOpeningBalance> ytdOpeningByEmp = ytdOpeningRepo.findByCompanyIdAndTaxYear(company.getId(), period.getPeriodYear()).stream()
                .collect(Collectors.toMap(EmployeeYtdOpeningBalance::getEmploymentId, Function.identity()));

        // Gather prior month results in the same year (e.g., Nov results when running Dec)
        Map<UUID, PriorYtdAccumulation> priorMonthDbYtd = buildPriorMonthYtdAccumulation(company.getId(), period);

        // Gather same-month prior run accumulations (for Multi-Run BR-TAX-009 & BPJS 1x/month)
        Map<UUID, SameMonthPriorRun> sameMonthPrior = buildSameMonthPrior(otherRunsSameMonth);

        // Create PayrollRun header
        long seq = runRepo.findByCompanyIdOrderByCreatedAtDesc(company.getId()).size() + 1L;
        String runNumber = String.format("PR-%d%02d-%s-%03d", period.getPeriodYear(), period.getPeriodMonth(), runType.substring(0, Math.min(3, runType.length())), seq);

        PayrollRun run = new PayrollRun();
        run.setId(UUID.randomUUID());
        run.setCompanyId(company.getId());
        run.setPayrollPeriodId(period.getId());
        run.setRunNumber(runNumber);
        run.setRunType(runType);
        run.setRuleSetVersionId(ruleSet.getId());
        run.setCompanyPolicyVersionId(policy.getId());
        run.setEngineVersion(ENGINE_VERSION);
        run.setStatus("CALCULATED");
        run.setSnapshotLockedAt(Instant.now());
        run.setCreatedAt(Instant.now());
        run.setTotalEmployees(0);
        run.setTotalGross(BigDecimal.ZERO);
        run.setTotalDeductions(BigDecimal.ZERO);
        run.setTotalNetPay(BigDecimal.ZERO);
        run.setTotalEmployerCost(BigDecimal.ZERO);
        runRepo.save(run);

        List<Employment> employments = employmentRepo.findByCompanyIdOrderByEmployeeNumberAsc(company.getId()).stream()
                .filter(em -> !em.getHireDate().isAfter(period.getPeriodEnd()))
                .filter(em -> em.getTerminationDate() == null || !em.getTerminationDate().isBefore(period.getPeriodStart()))
                .toList();

        BigDecimal runTotalGross = BigDecimal.ZERO;
        BigDecimal runTotalDeductions = BigDecimal.ZERO;
        BigDecimal runTotalNet = BigDecimal.ZERO;
        BigDecimal runTotalEmployerCost = BigDecimal.ZERO;

        List<PayrollResult> savedResults = new ArrayList<>();
        List<PayrollDetail> allDetailsToSave = new ArrayList<>();

        for (Employment em : employments) {
            Employee emp = employeeMap.get(em.getEmployeeId());
            WorkLocation loc = locMap.get(em.getWorkLocationId());
            AttendanceSummary att = attByEmp.get(em.getId());
            SimulatorOverride sim = overrideByEmp.get(em.getId());

            String effectiveTaxMethod = (sim != null && sim.getTaxMethodOverride() != null && !sim.getTaxMethodOverride().isBlank())
                    ? sim.getTaxMethodOverride() : em.getTaxMethod();

            List<EmployeeSalaryAssignment> assignments = assignmentRepo.findByEmploymentIdOrderByValidFromAsc(em.getId()).stream()
                    .filter(a -> !a.getValidFrom().isAfter(period.getPeriodEnd()) && (a.getValidTo() == null || !a.getValidTo().isBefore(period.getPeriodStart())))
                    .toList();

            List<PayrollDetail> empDetails = new ArrayList<>();
            UUID resultId = UUID.randomUUID();
            List<String> anomalies = new ArrayList<>();
            if (sim != null) {
                anomalies.add("SIMULATOR_OVERRIDE_ACTIVE");
            }

            // Stage 3: Evaluate Fixed Salary & Prorata Multi-Segment (REGULAR) or THR (THR run)
            BigDecimal basicAndFixedTotal = BigDecimal.ZERO;
            BigDecimal fullContractualFixedWage = BigDecimal.ZERO;
            Map<String, Object> prorataTrace = new LinkedHashMap<>();

            // Determine contractual fixed wage as of period end (for Overtime / BPJS / THR / PKWT base)
            LocalDate endRef = em.getTerminationDate() != null && !em.getTerminationDate().isAfter(period.getPeriodEnd())
                    ? em.getTerminationDate() : period.getPeriodEnd();
            for (EmployeeSalaryAssignment a : assignments) {
                SalaryComponent sc = compById.get(a.getSalaryComponentId());
                if (sc != null && sc.isFixed() && !a.getValidFrom().isAfter(endRef) && (a.getValidTo() == null || !a.getValidTo().isBefore(endRef))) {
                    fullContractualFixedWage = fullContractualFixedWage.add(a.getAmount());
                }
            }

            if ("REGULAR".equals(runType)) {
                int scheduledDays = att != null ? att.getScheduledWorkingDays() : period.getScheduledWorkingDays();
                int baseAttended = att != null ? att.getActualWorkingDays() + att.getUnpaidLeaveDays().intValue() : scheduledDays;
                BigDecimal effectiveUnpaid = (sim != null && sim.getUnpaidLeaveDaysOverride() != null)
                        ? sim.getUnpaidLeaveDaysOverride()
                        : (att != null ? att.getUnpaidLeaveDays() : BigDecimal.ZERO);
                int paidDays = Math.max(0, baseAttended - effectiveUnpaid.setScale(0, RoundingMode.HALF_UP).intValue());

                Map<UUID, List<EmployeeSalaryAssignment>> byComp = assignments.stream()
                        .collect(Collectors.groupingBy(EmployeeSalaryAssignment::getSalaryComponentId));

                List<Map<String, Object>> segmentTraceList = new ArrayList<>();
                for (Map.Entry<UUID, List<EmployeeSalaryAssignment>> entry : byComp.entrySet()) {
                    SalaryComponent sc = compById.get(entry.getKey());
                    if (sc == null || !"EARNING".equals(sc.getCategory())) continue;

                    List<ProrataCalculator.SalarySegment> segments = buildSegments(entry.getValue(), period, paidDays, scheduledDays);
                    ProrataCalculator.ProrataResult pr = prorataCalculator.calculateMultiSegment(segments, scheduledDays);
                    basicAndFixedTotal = basicAndFixedTotal.add(pr.totalAmount());

                    String formulaDesc = pr.segments().size() > 1
                            ? String.format("Multi-Segment Split (%d segmen, total %d/%d hari kerja)", pr.segments().size(), paidDays, scheduledDays)
                            : String.format("%d/%d hari kerja × Rp %s", paidDays, scheduledDays, entry.getValue().get(0).getAmount());

                    empDetails.add(buildDetail(resultId, sc.getCode(), sc.getName(), "EARNING",
                            entry.getValue().get(entry.getValue().size() - 1).getAmount(), null, pr.overallFactor(),
                            pr.totalAmount(), em.getCostCenterId(), formulaDesc));

                    for (ProrataCalculator.SegmentTrace st : pr.segments()) {
                        Map<String, Object> segMap = new LinkedHashMap<>();
                        segMap.put("component", sc.getCode());
                        segMap.put("from", st.from().toString());
                        segMap.put("to", st.to().toString());
                        segMap.put("paidDays", st.paidDays());
                        segMap.put("totalPeriodDays", st.totalScheduledDays());
                        segMap.put("factor", st.factor());
                        segMap.put("monthlyAmount", st.monthlyAmount());
                        segMap.put("exactAmount", st.exactAmount());
                        segmentTraceList.add(segMap);
                    }
                }
                prorataTrace.put("method", policy.getDefaultProrateMethod());
                prorataTrace.put("scheduledWorkingDays", scheduledDays);
                prorataTrace.put("paidWorkingDays", paidDays);
                prorataTrace.put("segments", segmentTraceList);
            } else if ("THR".equals(runType)) {
                long tenureMonths = ChronoUnit.MONTHS.between(em.getHireDate().withDayOfMonth(1), period.getPeriodEnd().plusDays(1).withDayOfMonth(1));
                if (tenureMonths < 1 && ChronoUnit.DAYS.between(em.getHireDate(), period.getPeriodEnd()) >= 30) {
                    tenureMonths = 1;
                }
                BigDecimal thrAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                String thrFormula;
                if (tenureMonths >= 12) {
                    thrAmount = fullContractualFixedWage.setScale(2, RoundingMode.HALF_UP);
                    thrFormula = "Masa kerja >= 12 bulan -> 1 × Upah Tetap";
                } else if (tenureMonths >= 1) {
                    thrAmount = fullContractualFixedWage.multiply(BigDecimal.valueOf(tenureMonths))
                            .divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP)
                            .setScale(2, RoundingMode.HALF_UP);
                    thrFormula = String.format("Proporsional (%d/12) × Upah Tetap", tenureMonths);
                } else {
                    thrFormula = "Masa kerja < 1 bulan (belum eligible THR)";
                }
                if (thrAmount.compareTo(BigDecimal.ZERO) > 0) {
                    SalaryComponent thrComp = compByCode.get("THR");
                    empDetails.add(buildDetail(resultId, "THR", thrComp.getName(), "EARNING",
                            fullContractualFixedWage, null, BigDecimal.ONE, thrAmount, em.getCostCenterId(), thrFormula));
                    basicAndFixedTotal = basicAndFixedTotal.add(thrAmount);
                }
                prorataTrace.put("runType", "THR");
                prorataTrace.put("tenureMonths", tenureMonths);
            }

            // Stage 4: Overtime (PP 35/2021), Variable Bonus, Reimbursement, & PKWT Compensation
            BigDecimal overtimeAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            OvertimeCalculator.OvertimeResult otRes = null;
            if ("REGULAR".equals(runType)) {
                BigDecimal weekdayOt = (sim != null && sim.getWeekdayOvertimeHoursOverride() != null)
                        ? sim.getWeekdayOvertimeHoursOverride()
                        : (att != null ? att.getWeekdayOvertimeHours() : BigDecimal.ZERO);
                BigDecimal holidayOt = (sim != null && sim.getHolidayOvertimeHoursOverride() != null)
                        ? sim.getHolidayOvertimeHoursOverride()
                        : (att != null ? att.getHolidayOvertimeHours() : BigDecimal.ZERO);

                otRes = overtimeCalculator.calculate(fullContractualFixedWage, fullContractualFixedWage, em.isOvertimeEligible(), weekdayOt, holidayOt);
                overtimeAmount = otRes.totalAmount();
                if (overtimeAmount.compareTo(BigDecimal.ZERO) > 0) {
                    SalaryComponent otComp = compByCode.get("OVERTIME");
                    empDetails.add(buildDetail(resultId, "OVERTIME", otComp.getName(), "EARNING",
                            otRes.hourlyRate(), null, BigDecimal.ONE, overtimeAmount, em.getCostCenterId(),
                            String.format("Upah/jam Rp %s (1/173 × Rp %s); HK %.1f jam + HL %.1f jam",
                                    otRes.hourlyRate(), otRes.baseWageUsed(), weekdayOt, holidayOt)));
                }
            }

            BigDecimal bonusAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal reimbursementAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if ("REGULAR".equals(runType)) {
                List<PayrollVariableInput> empVars = varsByEmp.getOrDefault(em.getId(), List.of());
                for (PayrollVariableInput vi : empVars) {
                    SalaryComponent sc = compById.get(vi.getSalaryComponentId());
                    if (sc == null) continue;
                    if ("REIMBURSEMENT".equals(sc.getCategory())) {
                        reimbursementAmount = reimbursementAmount.add(vi.getAmount());
                        empDetails.add(buildDetail(resultId, sc.getCode(), sc.getName(), "REIMBURSEMENT",
                                vi.getAmount(), null, BigDecimal.ONE, vi.getAmount(), em.getCostCenterId(),
                                "Non-upah & non-pajak: " + (vi.getNotes() != null ? vi.getNotes() : "")));
                    } else if ("EARNING".equals(sc.getCategory())) {
                        bonusAmount = bonusAmount.add(vi.getAmount());
                    }
                }
                if (sim != null && sim.getBonusOverride() != null) {
                    bonusAmount = sim.getBonusOverride().setScale(2, RoundingMode.HALF_UP);
                }
                if (bonusAmount.compareTo(BigDecimal.ZERO) > 0) {
                    SalaryComponent bonusComp = compByCode.get("VARIABLE_BONUS");
                    empDetails.add(buildDetail(resultId, "VARIABLE_BONUS", bonusComp.getName(), "EARNING",
                            bonusAmount, null, BigDecimal.ONE, bonusAmount, em.getCostCenterId(),
                            sim != null && sim.getBonusOverride() != null ? "Simulasi Bonus Variabel" : "Input Variabel Periode"));
                }
            }

            // PKWT Compensation (PP 35/2021 Pasal 16) when PKWT ends in current period
            BigDecimal pkwtCompAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if ("REGULAR".equals(runType) && "PKWT".equals(em.getContractType()) && em.getTerminationDate() != null
                    && !em.getTerminationDate().isBefore(period.getPeriodStart()) && !em.getTerminationDate().isAfter(period.getPeriodEnd())
                    && "PKWT_EXPIRED".equals(em.getTerminationReason())) {
                long contractMonths = Math.max(1, ChronoUnit.MONTHS.between(em.getHireDate().withDayOfMonth(1), em.getTerminationDate().plusDays(1).withDayOfMonth(1)));
                pkwtCompAmount = fullContractualFixedWage.multiply(BigDecimal.valueOf(contractMonths))
                        .divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP)
                        .setScale(2, RoundingMode.HALF_UP);
                SalaryComponent pkwtComp = compByCode.get("PKWT_COMPENSATION");
                empDetails.add(buildDetail(resultId, "PKWT_COMPENSATION", pkwtComp.getName(), "EARNING",
                        fullContractualFixedWage, null, BigDecimal.valueOf(contractMonths).divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP),
                        pkwtCompAmount, em.getCostCenterId(),
                        String.format("PP 35/2021: (%d bln / 12) × Rp %s", contractMonths, fullContractualFixedWage)));
            }

            BigDecimal earningsBeforeTaxAllowance = basicAndFixedTotal.add(overtimeAmount).add(bonusAmount).add(pkwtCompAmount);

            // Stage 5: BPJS Kesehatan & 5 Program BPJS TK (Single Monthly Deduction check BR-BPJS-TK-007)
            SameMonthPriorRun priorThisMonth = sameMonthPrior.getOrDefault(em.getId(), SameMonthPriorRun.EMPTY);
            boolean bpjsAlreadyDeducted = priorThisMonth.bpjsAlreadyCalculated();
            int ageYears = Period.between(emp.getBirthDate(), period.getPeriodEnd()).getYears();

            BpjsCalculator.BpjsResult bpjs = bpjsCalculator.calculate(
                    fullContractualFixedWage,
                    fullContractualFixedWage,
                    loc.getUmpAmount(),
                    em.getBpjsKesExtraDependents(),
                    ageYears,
                    emp.getNationality(),
                    em.isJkpEligible(),
                    bpjsConfigs,
                    bpjsAlreadyDeducted
            );

            if (!bpjsAlreadyDeducted) {
                empDetails.add(buildDetail(resultId, "BPJS_KES_EMP", "BPJS Kesehatan (Pekerja 1% + Tambahan)", "DEDUCTION",
                        bpjs.kesCappedBase(), new BigDecimal("0.010000"), BigDecimal.ONE, bpjs.kesEmployee().add(bpjs.kesExtraDependent()),
                        em.getCostCenterId(), String.format("1%% × Rp %s + %d tanggungan tambahan", bpjs.kesCappedBase(), em.getBpjsKesExtraDependents())));
                empDetails.add(buildDetail(resultId, "BPJS_KES_ER", "BPJS Kesehatan (Pemberi Kerja 4%)", "EMPLOYER_CONTRIBUTION",
                        bpjs.kesCappedBase(), new BigDecimal("0.040000"), BigDecimal.ONE, bpjs.kesEmployer(),
                        em.getCostCenterId(), "4% × Basis Terbatas (penambah bruto pajak)"));
                empDetails.add(buildDetail(resultId, "BPJS_JHT_EMP", "BPJS TK — JHT Pekerja (2%)", "DEDUCTION",
                        bpjs.jhtBase(), new BigDecimal("0.020000"), BigDecimal.ONE, bpjs.jhtEmployee(),
                        em.getCostCenterId(), "2% × Upah Tetap (tanpa batas atas)"));
                empDetails.add(buildDetail(resultId, "BPJS_JHT_ER", "BPJS TK — JHT Pemberi Kerja (3.7%)", "EMPLOYER_CONTRIBUTION",
                        bpjs.jhtBase(), new BigDecimal("0.037000"), BigDecimal.ONE, bpjs.jhtEmployer(),
                        em.getCostCenterId(), "3.7% × Upah Tetap"));
                empDetails.add(buildDetail(resultId, "BPJS_JKK_ER", "BPJS TK — JKK Pemberi Kerja (0.24%)", "EMPLOYER_CONTRIBUTION",
                        bpjs.jhtBase(), new BigDecimal("0.002400"), BigDecimal.ONE, bpjs.jkkEmployer(),
                        em.getCostCenterId(), "0.24% × Upah Tetap (Kelas Risiko I, penambah bruto pajak)"));
                empDetails.add(buildDetail(resultId, "BPJS_JKM_ER", "BPJS TK — JKM Pemberi Kerja (0.30%)", "EMPLOYER_CONTRIBUTION",
                        bpjs.jhtBase(), new BigDecimal("0.003000"), BigDecimal.ONE, bpjs.jkmEmployer(),
                        em.getCostCenterId(), "0.30% × Upah Tetap (penambah bruto pajak)"));
                empDetails.add(buildDetail(resultId, "BPJS_JP_EMP", "BPJS TK — JP Pekerja (1%)", "DEDUCTION",
                        bpjs.jpCappedBase(), new BigDecimal("0.010000"), BigDecimal.ONE, bpjs.jpEmployee(),
                        em.getCostCenterId(), String.format("1%% × min(Upah, Cap Rp %s)", bpjs.jpCapLimit())));
                empDetails.add(buildDetail(resultId, "BPJS_JP_ER", "BPJS TK — JP Pemberi Kerja (2%)", "EMPLOYER_CONTRIBUTION",
                        bpjs.jpCappedBase(), new BigDecimal("0.020000"), BigDecimal.ONE, bpjs.jpEmployer(),
                        em.getCostCenterId(), "2% × Basis Terbatas JP"));
                empDetails.add(buildDetail(resultId, "BPJS_JKP_STATUS", "BPJS TK — Status JKP (Non-Iuran Tambahan)", "MEMO",
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO,
                        em.getCostCenterId(), bpjs.jkpEligible() ? "ELIGIBLE (Rekomposisi JKK/JKM & Pemerintah, 0 iuran tambahan)" : "NOT ELIGIBLE"));
            }

            // Stage 6 & 7: PPh 21 (TER Multi-Run vs Masa Pajak Terakhir Pasal 17 + Gross-Up)
            BigDecimal baseGrossTaxable = earningsBeforeTaxAllowance.add(bpjs.totalTaxableEmployerPremi());
            boolean isTerminatingThisPeriod = em.getTerminationDate() != null
                    && !em.getTerminationDate().isBefore(period.getPeriodStart())
                    && !em.getTerminationDate().isAfter(period.getPeriodEnd());
            boolean isFinalTaxPeriodForEmp = period.isFinalTaxPeriod() || isTerminatingThisPeriod;

            BigDecimal pph21Amount;
            BigDecimal taxAllowance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            Map<String, Object> taxTrace = new LinkedHashMap<>();

            if (!isFinalTaxPeriodForEmp) {
                if ("GROSS_UP".equals(effectiveTaxMethod)) {
                    Pph21Calculator.GrossUpResult gu = pph21Calculator.solveMonthlyTerGrossUp(
                            em.getTerCategory(), baseGrossTaxable, priorThisMonth.grossTaxable(), priorThisMonth.taxWithheld(), terBrackets);
                    taxAllowance = gu.taxAllowance();
                    pph21Amount = gu.pph21Amount();
                    taxTrace.put("mode", "MONTHLY_TER_GROSS_UP");
                    taxTrace.put("iterations", gu.iterationsUsed());
                    taxTrace.put("terRateApplied", gu.terResult().terRateApplied());
                    taxTrace.put("mtdPriorGross", gu.terResult().mtdPriorGross());
                    taxTrace.put("mtdTotalGrossTaxable", gu.terResult().mtdTotalGrossTaxable());
                } else {
                    Pph21Calculator.MonthlyTerResult terRes = pph21Calculator.calculateMonthlyTer(
                            em.getTerCategory(), baseGrossTaxable, priorThisMonth.grossTaxable(), priorThisMonth.taxWithheld(), terBrackets);
                    pph21Amount = terRes.currentTaxAmount();
                    taxTrace.put("mode", priorThisMonth.grossTaxable().compareTo(BigDecimal.ZERO) > 0 ? "MONTHLY_TER_MULTI_RUN" : "MONTHLY_TER");
                    taxTrace.put("terRateApplied", terRes.terRateApplied());
                    taxTrace.put("mtdPriorGross", terRes.mtdPriorGross());
                    taxTrace.put("mtdPriorTaxWithheld", terRes.mtdPriorTaxWithheld());
                    taxTrace.put("mtdTotalGrossTaxable", terRes.mtdTotalGrossTaxable());
                }
            } else {
                // Masa Pajak Terakhir (Desember / Resign / PKWT Berakhir): Rekonsiliasi Pasal 17 UU HPP
                EmployeeYtdOpeningBalance ytdOpen = ytdOpeningByEmp.get(em.getId());
                PriorYtdAccumulation dbPrior = priorMonthDbYtd.getOrDefault(em.getId(), PriorYtdAccumulation.EMPTY);

                BigDecimal ytdPriorGross = (ytdOpen != null ? ytdOpen.getYtdGrossTaxable() : BigDecimal.ZERO)
                        .add(dbPrior.grossTaxable())
                        .add(priorThisMonth.grossTaxable());
                BigDecimal ytdPriorPension = (ytdOpen != null ? ytdOpen.getYtdBpjsWorkerPension() : BigDecimal.ZERO)
                        .add(dbPrior.workerPension())
                        .add(priorThisMonth.workerPension());
                BigDecimal ytdPriorTax = (ytdOpen != null ? ytdOpen.getYtdPph21Withheld() : BigDecimal.ZERO)
                        .add(dbPrior.taxWithheld())
                        .add(priorThisMonth.taxWithheld());

                int openingMonths = ytdOpen != null ? ytdOpen.getCutoffMonthCount() : 0;
                int activeMonths = Math.min(12, openingMonths + dbPrior.distinctMonthCount() + 1);
                BigDecimal currentJhtJp = bpjs.jhtEmployee().add(bpjs.jpEmployee());
                BigDecimal totalAnnualJhtJp = ytdPriorPension.add(currentJhtJp);
                BigDecimal annualGrossBeforeCurrentAllowance = ytdPriorGross.add(baseGrossTaxable);

                Pph21Calculator.Pasal17Result p17;
                if ("GROSS_UP".equals(effectiveTaxMethod)) {
                    Pph21Calculator.GrossUpResult gu = pph21Calculator.solvePasal17GrossUp(
                            em.getPtkpStatus(), annualGrossBeforeCurrentAllowance, activeMonths, totalAnnualJhtJp, ytdPriorTax, pasal17Brackets);
                    taxAllowance = gu.taxAllowance();
                    pph21Amount = gu.pph21Amount();
                    p17 = gu.pasal17Result();
                    taxTrace.put("iterations", gu.iterationsUsed());
                } else {
                    p17 = pph21Calculator.calculatePasal17Final(
                            em.getPtkpStatus(), annualGrossBeforeCurrentAllowance, activeMonths, totalAnnualJhtJp, ytdPriorTax, pasal17Brackets);
                    pph21Amount = p17.currentTaxAmount();
                }

                if (p17.isRefund()) {
                    anomalies.add("TAX_REFUND_FINAL_PERIOD");
                }
                taxTrace.put("mode", "FINAL_PERIOD_PASAL_17");
                taxTrace.put("activeMonthsInYear", activeMonths);
                taxTrace.put("ytdPriorGross", ytdPriorGross);
                taxTrace.put("annualGrossTaxable", p17.annualGrossTaxable());
                taxTrace.put("biayaJabatan", p17.biayaJabatan());
                taxTrace.put("annualJhtJpWorker", p17.annualJhtJpWorker());
                taxTrace.put("annualNeto", p17.annualNeto());
                taxTrace.put("ptkpAnnual", p17.ptkpAnnual());
                taxTrace.put("pkpAnnual", p17.pkpAnnual());
                taxTrace.put("annualTaxDue", p17.annualTaxDue());
                taxTrace.put("previouslyWithheld", p17.previouslyWithheld());
                taxTrace.put("isRefund", p17.isRefund());
                taxTrace.put("layers", p17.layers());
            }

            if (taxAllowance.compareTo(BigDecimal.ZERO) > 0) {
                SalaryComponent taComp = compByCode.get("TAX_ALLOWANCE");
                empDetails.add(buildDetail(resultId, "TAX_ALLOWANCE", taComp.getName(), "EARNING",
                        taxAllowance, null, BigDecimal.ONE, taxAllowance, em.getCostCenterId(),
                        "Tunjangan Pajak Gross-Up konvergen (Tunjangan Pajak == PPh 21)"));
            }

            BigDecimal pphEmployeeDeduction = "NET".equals(effectiveTaxMethod) ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : pph21Amount;
            BigDecimal employerTaxExpense = "NET".equals(effectiveTaxMethod) ? pph21Amount.max(BigDecimal.ZERO) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            if ("NET".equals(effectiveTaxMethod)) {
                empDetails.add(buildDetail(resultId, "PPH21_NET_EMPLOYER", "PPh 21 Ditanggung Perusahaan (NET)", "EMPLOYER_CONTRIBUTION",
                        baseGrossTaxable, null, BigDecimal.ONE, employerTaxExpense, em.getCostCenterId(),
                        "Metode NET: PPh 21 ditanggung perusahaan, tidak memotong Net Pay"));
            } else {
                String pphLabel = pph21Amount.compareTo(BigDecimal.ZERO) < 0
                        ? "Pengembalian Lebih Potong PPh 21 (Tax Refund Pasal 17)"
                        : "Potongan PPh Pasal 21 (" + effectiveTaxMethod + ")";
                empDetails.add(buildDetail(resultId, "PPH21", pphLabel, "DEDUCTION",
                        baseGrossTaxable.add(taxAllowance), null, BigDecimal.ONE, pphEmployeeDeduction, em.getCostCenterId(),
                        isFinalTaxPeriodForEmp ? "Rekonsiliasi Pasal 17 Masa Pajak Terakhir" : "Tarif TER PP 58/2023"));
            }

            // Stage 8: Loan Deduction & 50% Cap Validation
            BigDecimal grossEarnings = earningsBeforeTaxAllowance.add(taxAllowance).setScale(2, RoundingMode.HALF_UP);
            BigDecimal loanDeduction = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if ("REGULAR".equals(runType)) {
                EmployeeLoan loan = loanByEmp.get(em.getId());
                if (loan != null && loan.getRemainingBalance().compareTo(BigDecimal.ZERO) > 0) {
                    loanDeduction = loan.getMonthlyInstallment().min(loan.getRemainingBalance()).setScale(2, RoundingMode.HALF_UP);
                    if (grossEarnings.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal ratioPct = loanDeduction.multiply(BigDecimal.valueOf(100)).divide(grossEarnings, 2, RoundingMode.HALF_UP);
                        if (ratioPct.compareTo(policy.getMaxNonStatutoryDedPct()) > 0) {
                            anomalies.add("DEDUCTION_OVER_50_PCT");
                        }
                    }
                    empDetails.add(buildDetail(resultId, "LOAN_INSTALLMENT", "Cicilan Pinjaman (" + loan.getLoanNumber() + ")", "DEDUCTION",
                            loan.getRemainingBalance(), null, BigDecimal.ONE, loanDeduction, em.getCostCenterId(),
                            "Sisa pinjaman sebelum potong: Rp " + loan.getRemainingBalance()));
                }
            }

            // Stage 9-12: Net Pay, Employer Cost & Bank Hold
            BigDecimal totalDeductions = bpjs.totalEmployeeDeduction().add(pphEmployeeDeduction).add(loanDeduction).setScale(2, RoundingMode.HALF_UP);
            BigDecimal netPay = grossEarnings.subtract(totalDeductions).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalTakeHome = netPay.add(reimbursementAmount).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalEmployerCost = grossEarnings.add(reimbursementAmount).add(bpjs.totalEmployerCost()).add(employerTaxExpense).setScale(2, RoundingMode.HALF_UP);

            if (netPay.compareTo(BigDecimal.ZERO) < 0) {
                anomalies.add("NEGATIVE_NET_PAY");
            }
            String paymentStatus = "PREPARED";
            if (!"VERIFIED".equals(emp.getBankVerificationStatus())) {
                paymentStatus = "ON_HOLD";
                anomalies.add("UNVERIFIED_BANK_ACCOUNT");
            }

            // Build Calculation Trace JSON
            taxTrace.put("ptkpStatus", em.getPtkpStatus());
            taxTrace.put("terCategory", em.getTerCategory());
            taxTrace.put("taxMethod", effectiveTaxMethod);
            taxTrace.put("currentRunGrossTaxable", baseGrossTaxable.add(taxAllowance));
            taxTrace.put("taxAllowance", taxAllowance);
            taxTrace.put("pph21Amount", pph21Amount);

            Map<String, Object> traceRoot = buildTraceMap(ruleSet, policy, period, runType, em, emp, sim != null,
                    prorataTrace, otRes, bpjs, taxTrace, grossEarnings, reimbursementAmount, totalDeductions, netPay, totalTakeHome, totalEmployerCost, anomalies);

            PayrollResult result = new PayrollResult();
            result.setId(resultId);
            result.setCompanyId(company.getId());
            result.setPayrollRunId(run.getId());
            result.setEmploymentId(em.getId());
            result.setCostCenterId(em.getCostCenterId());
            result.setEmployeeNumber(em.getEmployeeNumber());
            result.setEmployeeName(emp.getFullName());
            result.setScenarioTitle(em.getScenarioTitle());
            result.setContractType(em.getContractType());
            result.setPtkpStatus(em.getPtkpStatus());
            result.setTerCategory(em.getTerCategory());
            result.setTaxMethod(effectiveTaxMethod);
            result.setGrossEarnings(grossEarnings);
            result.setTotalReimbursements(reimbursementAmount);
            result.setBpjsEmployeeTotal(bpjs.totalEmployeeDeduction());
            result.setBpjsEmployerTotal(bpjs.totalEmployerCost());
            result.setPph21Amount(pph21Amount);
            result.setTaxAllowance(taxAllowance);
            result.setLoanDeduction(loanDeduction);
            result.setTotalDeductions(totalDeductions);
            result.setNetPay(netPay);
            result.setTotalTakeHomeTransfer(totalTakeHome);
            result.setTotalEmployerCost(totalEmployerCost);
            result.setPaymentStatus(paymentStatus);
            result.setAnomalyFlagsJson(toJson(anomalies));
            result.setCalculationTraceJson(toJson(traceRoot));

            savedResults.add(result);
            allDetailsToSave.addAll(empDetails);

            runTotalGross = runTotalGross.add(grossEarnings);
            runTotalDeductions = runTotalDeductions.add(totalDeductions);
            runTotalNet = runTotalNet.add(netPay);
            runTotalEmployerCost = runTotalEmployerCost.add(totalEmployerCost);
        }

        resultRepo.saveAll(savedResults);
        detailRepo.saveAll(allDetailsToSave);

        run.setTotalEmployees(savedResults.size());
        run.setTotalGross(runTotalGross.setScale(2, RoundingMode.HALF_UP));
        run.setTotalDeductions(runTotalDeductions.setScale(2, RoundingMode.HALF_UP));
        run.setTotalNetPay(runTotalNet.setScale(2, RoundingMode.HALF_UP));
        run.setTotalEmployerCost(runTotalEmployerCost.setScale(2, RoundingMode.HALF_UP));
        runRepo.save(run);

        // Stage 13b: Generate Balanced Accounting Journal per Cost Center
        generateBalancedJournal(company, period, run, savedResults, ccMap);

        try {
            redisTemplate.opsForValue().set("payroll:run:latest:" + period.getId(), run.getId().toString(), Duration.ofHours(6));
            redisTemplate.delete(redisLockKey);
        } catch (Exception e) {
            log.debug("Redis cleanup skipped: {}", e.getMessage());
        }

        return toRunDto(run, period);
    }

    private void generateBalancedJournal(
            Company company,
            PayrollPeriod period,
            PayrollRun run,
            List<PayrollResult> results,
            Map<UUID, CostCenter> ccMap
    ) {
        Journal journal = new Journal();
        journal.setId(UUID.randomUUID());
        journal.setCompanyId(company.getId());
        journal.setPayrollRunId(run.getId());
        journal.setJournalNumber("JRN-" + run.getRunNumber());
        journal.setPostingDate(period.getPayDate());
        journal.setStatus("DRAFT");

        Map<UUID, List<PayrollResult>> byCc = results.stream()
                .collect(Collectors.groupingBy(PayrollResult::getCostCenterId, LinkedHashMap::new, Collectors.toList()));

        List<JournalLine> lines = new ArrayList<>();
        int lineNo = 1;
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (Map.Entry<UUID, List<PayrollResult>> entry : byCc.entrySet()) {
            CostCenter cc = ccMap.get(entry.getKey());
            List<PayrollResult> ccResults = entry.getValue();

            BigDecimal ccGross = sum(ccResults, PayrollResult::getGrossEarnings);
            BigDecimal ccReimb = sum(ccResults, PayrollResult::getTotalReimbursements);
            BigDecimal ccBpjsEr = sum(ccResults, PayrollResult::getBpjsEmployerTotal);
            BigDecimal ccBpjsEmp = sum(ccResults, PayrollResult::getBpjsEmployeeTotal);
            BigDecimal ccNetEmployerTax = ccResults.stream()
                    .filter(r -> "NET".equals(r.getTaxMethod()))
                    .map(r -> r.getPph21Amount().max(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal ccPphTotalPayable = ccResults.stream()
                    .map(PayrollResult::getPph21Amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal ccLoan = sum(ccResults, PayrollResult::getLoanDeduction);
            BigDecimal ccTakeHome = sum(ccResults, PayrollResult::getTotalTakeHomeTransfer);

            if (ccGross.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(jLine(journal.getId(), lineNo++, "5100-GAJI", "Beban Gaji, Tunjangan & Upah", cc, ccGross, BigDecimal.ZERO, "Akrual upah " + period.getPeriodName()));
                totalDebit = totalDebit.add(ccGross);
            }
            if (ccReimb.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(jLine(journal.getId(), lineNo++, "5109-REIMB", "Beban Reimbursement Non-Upah", cc, ccReimb, BigDecimal.ZERO, "Klaim reimbursement non-upah"));
                totalDebit = totalDebit.add(ccReimb);
            }
            if (ccBpjsEr.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(jLine(journal.getId(), lineNo++, "5201-BPJS-ER", "Beban Iuran BPJS Perusahaan", cc, ccBpjsEr, BigDecimal.ZERO, "BPJS Kes 4% + JHT 3.7% + JKK + JKM + JP 2%"));
                totalDebit = totalDebit.add(ccBpjsEr);
            }
            if (ccNetEmployerTax.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(jLine(journal.getId(), lineNo++, "5202-PPH-NET", "Beban PPh 21 Ditanggung Perusahaan", cc, ccNetEmployerTax, BigDecimal.ZERO, "PPh 21 metode NET"));
                totalDebit = totalDebit.add(ccNetEmployerTax);
            }

            if (ccTakeHome.compareTo(BigDecimal.ZERO) >= 0) {
                lines.add(jLine(journal.getId(), lineNo++, "2101-UTANG-GAJI", "Utang Gaji Bersih Karyawan", cc, BigDecimal.ZERO, ccTakeHome, "Take-home pay ke transfer bank"));
                totalCredit = totalCredit.add(ccTakeHome);
            } else {
                lines.add(jLine(journal.getId(), lineNo++, "2101-UTANG-GAJI", "Piutang Kelebihan Potong Karyawan", cc, ccTakeHome.abs(), BigDecimal.ZERO, "Saldo net negatif"));
                totalDebit = totalDebit.add(ccTakeHome.abs());
            }

            if (ccPphTotalPayable.compareTo(BigDecimal.ZERO) >= 0) {
                if (ccPphTotalPayable.compareTo(BigDecimal.ZERO) > 0) {
                    lines.add(jLine(journal.getId(), lineNo++, "2102-UTANG-PPH21", "Utang PPh Pasal 21 (DJP)", cc, BigDecimal.ZERO, ccPphTotalPayable, "Setoran kas negara maks tgl 15"));
                    totalCredit = totalCredit.add(ccPphTotalPayable);
                }
            } else {
                lines.add(jLine(journal.getId(), lineNo++, "2102-UTANG-PPH21", "Koreksi Lebih Potong PPh 21 (Refund)", cc, ccPphTotalPayable.abs(), BigDecimal.ZERO, "Pengembalian lebih potong Pasal 17"));
                totalDebit = totalDebit.add(ccPphTotalPayable.abs());
            }

            BigDecimal ccBpjsPayable = ccBpjsEr.add(ccBpjsEmp);
            if (ccBpjsPayable.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(jLine(journal.getId(), lineNo++, "2103-UTANG-BPJS", "Utang Iuran BPJS Kes & TK", cc, BigDecimal.ZERO, ccBpjsPayable, "Total iuran pemberi kerja + potongan pekerja"));
                totalCredit = totalCredit.add(ccBpjsPayable);
            }
            if (ccLoan.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(jLine(journal.getId(), lineNo++, "1151-PIUTANG-PINJAMAN", "Pelunasan Piutang Pinjaman Karyawan", cc, BigDecimal.ZERO, ccLoan, "Potongan cicilan pinjaman"));
                totalCredit = totalCredit.add(ccLoan);
            }
        }

        journal.setTotalDebit(totalDebit.setScale(2, RoundingMode.HALF_UP));
        journal.setTotalCredit(totalCredit.setScale(2, RoundingMode.HALF_UP));
        journalRepo.save(journal);
        journalLineRepo.saveAll(lines);
    }

    private JournalLine jLine(UUID journalId, int lineNo, String coa, String accName, CostCenter cc, BigDecimal debit, BigDecimal credit, String desc) {
        JournalLine l = new JournalLine();
        l.setId(UUID.randomUUID());
        l.setJournalId(journalId);
        l.setLineNo(lineNo);
        l.setCoaAccountCode(coa);
        l.setAccountName(accName);
        l.setCostCenterId(cc.getId());
        l.setCostCenterCode(cc.getCode());
        l.setDebit(debit.setScale(2, RoundingMode.HALF_UP));
        l.setCredit(credit.setScale(2, RoundingMode.HALF_UP));
        l.setDescription(desc);
        return l;
    }

    private List<ProrataCalculator.SalarySegment> buildSegments(
            List<EmployeeSalaryAssignment> compAssignments,
            PayrollPeriod period,
            int totalPaidDays,
            int totalScheduledDays
    ) {
        if (compAssignments.size() == 1) {
            EmployeeSalaryAssignment a = compAssignments.get(0);
            LocalDate from = a.getValidFrom().isAfter(period.getPeriodStart()) ? a.getValidFrom() : period.getPeriodStart();
            LocalDate to = (a.getValidTo() != null && a.getValidTo().isBefore(period.getPeriodEnd())) ? a.getValidTo() : period.getPeriodEnd();
            return List.of(new ProrataCalculator.SalarySegment(from, to, totalPaidDays, a.getAmount()));
        }

        List<EmployeeSalaryAssignment> sorted = compAssignments.stream()
                .sorted(Comparator.comparing(EmployeeSalaryAssignment::getValidFrom))
                .toList();

        List<ProrataCalculator.SalarySegment> segments = new ArrayList<>();
        int assignedPaid = 0;
        int totalMonFri = countWeekdays(period.getPeriodStart(), period.getPeriodEnd());

        for (int i = 0; i < sorted.size(); i++) {
            EmployeeSalaryAssignment a = sorted.get(i);
            LocalDate from = a.getValidFrom().isAfter(period.getPeriodStart()) ? a.getValidFrom() : period.getPeriodStart();
            LocalDate to = (a.getValidTo() != null && a.getValidTo().isBefore(period.getPeriodEnd())) ? a.getValidTo() : period.getPeriodEnd();
            int segDays;
            if (i == sorted.size() - 1) {
                segDays = Math.max(0, totalPaidDays - assignedPaid);
            } else {
                int rawWeekdays = countWeekdays(from, to);
                segDays = totalMonFri > 0
                        ? Math.round((float) rawWeekdays * totalPaidDays / totalMonFri)
                        : rawWeekdays;
                assignedPaid += segDays;
            }
            segments.add(new ProrataCalculator.SalarySegment(from, to, segDays, a.getAmount()));
        }
        return segments;
    }

    private int countWeekdays(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) return 0;
        int count = 0;
        LocalDate d = start;
        while (!d.isAfter(end)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
            d = d.plusDays(1);
        }
        return count;
    }

    private record PriorYtdAccumulation(BigDecimal grossTaxable, BigDecimal workerPension, BigDecimal taxWithheld, int distinctMonthCount) {
        static final PriorYtdAccumulation EMPTY = new PriorYtdAccumulation(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
    }

    private record SameMonthPriorRun(BigDecimal grossTaxable, BigDecimal workerPension, BigDecimal taxWithheld, boolean bpjsAlreadyCalculated) {
        static final SameMonthPriorRun EMPTY = new SameMonthPriorRun(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false);
    }

    private Map<UUID, PriorYtdAccumulation> buildPriorMonthYtdAccumulation(UUID companyId, PayrollPeriod currentPeriod) {
        List<PayrollPeriod> priorPeriods = periodRepo.findByCompanyIdOrderByPeriodYearAscPeriodMonthAsc(companyId).stream()
                .filter(p -> p.getPeriodYear().equals(currentPeriod.getPeriodYear()) && p.getPeriodMonth() < currentPeriod.getPeriodMonth())
                .toList();

        Map<UUID, BigDecimal> grossMap = new HashMap<>();
        Map<UUID, BigDecimal> pensionMap = new HashMap<>();
        Map<UUID, BigDecimal> taxMap = new HashMap<>();
        Map<UUID, Set<Short>> monthMap = new HashMap<>();

        for (PayrollPeriod pp : priorPeriods) {
            List<PayrollRun> activeRunsInMonth = runRepo.findByPayrollPeriodIdOrderByCreatedAtAsc(pp.getId()).stream()
                    .filter(r -> !"CANCELLED".equals(r.getStatus()))
                    .toList();
            if (activeRunsInMonth.isEmpty()) continue;

            // Pick latest run per runType in that prior month
            Map<String, PayrollRun> latestByType = new LinkedHashMap<>();
            for (PayrollRun r : activeRunsInMonth) {
                latestByType.put(r.getRunType(), r);
            }
            for (PayrollRun r : latestByType.values()) {
                for (PayrollResult res : resultRepo.findByPayrollRunIdOrderByEmployeeNumberAsc(r.getId())) {
                    BigDecimal runGrossTaxable = extractTaxableGrossFromResult(res);
                    BigDecimal runWorkerPension = extractWorkerPensionFromDetails(res.getId());
                    grossMap.merge(res.getEmploymentId(), runGrossTaxable, BigDecimal::add);
                    pensionMap.merge(res.getEmploymentId(), runWorkerPension, BigDecimal::add);
                    taxMap.merge(res.getEmploymentId(), res.getPph21Amount(), BigDecimal::add);
                    monthMap.computeIfAbsent(res.getEmploymentId(), k -> new HashSet<>()).add(pp.getPeriodMonth());
                }
            }
        }

        Map<UUID, PriorYtdAccumulation> acc = new HashMap<>();
        for (UUID empId : grossMap.keySet()) {
            acc.put(empId, new PriorYtdAccumulation(
                    grossMap.getOrDefault(empId, BigDecimal.ZERO),
                    pensionMap.getOrDefault(empId, BigDecimal.ZERO),
                    taxMap.getOrDefault(empId, BigDecimal.ZERO),
                    monthMap.getOrDefault(empId, Set.of()).size()
            ));
        }
        return acc;
    }

    private Map<UUID, SameMonthPriorRun> buildSameMonthPrior(List<PayrollRun> otherRunsSameMonth) {
        if (otherRunsSameMonth.isEmpty()) return Map.of();
        Map<UUID, BigDecimal> grossMap = new HashMap<>();
        Map<UUID, BigDecimal> pensionMap = new HashMap<>();
        Map<UUID, BigDecimal> taxMap = new HashMap<>();
        Set<UUID> bpjsDone = new HashSet<>();

        for (PayrollRun r : otherRunsSameMonth) {
            for (PayrollResult res : resultRepo.findByPayrollRunIdOrderByEmployeeNumberAsc(r.getId())) {
                grossMap.merge(res.getEmploymentId(), extractTaxableGrossFromResult(res), BigDecimal::add);
                pensionMap.merge(res.getEmploymentId(), extractWorkerPensionFromDetails(res.getId()), BigDecimal::add);
                taxMap.merge(res.getEmploymentId(), res.getPph21Amount(), BigDecimal::add);
                if (res.getBpjsEmployeeTotal().compareTo(BigDecimal.ZERO) > 0 || res.getBpjsEmployerTotal().compareTo(BigDecimal.ZERO) > 0) {
                    bpjsDone.add(res.getEmploymentId());
                }
            }
        }

        Map<UUID, SameMonthPriorRun> map = new HashMap<>();
        for (UUID empId : grossMap.keySet()) {
            map.put(empId, new SameMonthPriorRun(
                    grossMap.getOrDefault(empId, BigDecimal.ZERO),
                    pensionMap.getOrDefault(empId, BigDecimal.ZERO),
                    taxMap.getOrDefault(empId, BigDecimal.ZERO),
                    bpjsDone.contains(empId)
            ));
        }
        return map;
    }

    private BigDecimal extractTaxableGrossFromResult(PayrollResult res) {
        Map<String, Object> trace = parseJsonMap(res.getCalculationTraceJson());
        Object pphObj = trace.get("pph21");
        if (pphObj instanceof Map<?, ?> pphMap && pphMap.get("currentRunGrossTaxable") != null) {
            return new BigDecimal(String.valueOf(pphMap.get("currentRunGrossTaxable")));
        }
        return res.getGrossEarnings();
    }

    private BigDecimal extractWorkerPensionFromDetails(UUID resultId) {
        return detailRepo.findByPayrollResultId(resultId).stream()
                .filter(d -> "BPJS_JHT_EMP".equals(d.getComponentCode()) || "BPJS_JP_EMP".equals(d.getComponentCode()))
                .map(PayrollDetail::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, Object> buildTraceMap(
            RuleSetVersion ruleSet, CompanyPolicyVersion policy, PayrollPeriod period, String runType,
            Employment em, Employee emp, boolean simulated,
            Map<String, Object> prorataTrace, OvertimeCalculator.OvertimeResult otRes, BpjsCalculator.BpjsResult bpjs,
            Map<String, Object> taxTrace, BigDecimal grossEarnings, BigDecimal reimbursement, BigDecimal totalDeductions,
            BigDecimal netPay, BigDecimal totalTakeHome, BigDecimal totalEmployerCost, List<String> anomalies
    ) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("engineVersion", ENGINE_VERSION);
        root.put("legalRuleVersionCode", ruleSet.getVersionCode());
        root.put("companyPolicyVersionCode", policy.getVersionCode());
        root.put("periodName", period.getPeriodName());
        root.put("runType", runType);
        root.put("employeeNumber", em.getEmployeeNumber());
        root.put("employeeName", emp.getFullName());
        root.put("simulated", simulated);
        root.put("calculatedAtUtc", Instant.now().toString());
        root.put("proration", prorataTrace);

        if (otRes != null && otRes.totalAmount().compareTo(BigDecimal.ZERO) > 0) {
            Map<String, Object> otMap = new LinkedHashMap<>();
            otMap.put("baseWageUsed", otRes.baseWageUsed());
            otMap.put("hourlyRate173", otRes.hourlyRate());
            otMap.put("weekdayAmount", otRes.weekdayAmount());
            otMap.put("holidayAmount", otRes.holidayAmount());
            otMap.put("totalOvertime", otRes.totalAmount());
            otMap.put("breakdown", otRes.breakdown());
            root.put("overtime", otMap);
        }

        Map<String, Object> bpjsMap = new LinkedHashMap<>();
        bpjsMap.put("singleMonthlyDeductionApplied", bpjs.singleMonthlyDeductionApplied());
        bpjsMap.put("kesehatan", Map.of(
                "rawBase", bpjs.kesRawBase(),
                "cappedBase", bpjs.kesCappedBase(),
                "employer4Pct", bpjs.kesEmployer(),
                "employee1Pct", bpjs.kesEmployee(),
                "extraDependentsAmount", bpjs.kesExtraDependent()
        ));
        Map<String, Object> tkMap = new LinkedHashMap<>();
        tkMap.put("JHT", Map.of("base", bpjs.jhtBase(), "employer37Pct", bpjs.jhtEmployer(), "employee20Pct", bpjs.jhtEmployee()));
        tkMap.put("JKK", Map.of("base", bpjs.jhtBase(), "employer024Pct", bpjs.jkkEmployer()));
        tkMap.put("JKM", Map.of("base", bpjs.jhtBase(), "employer030Pct", bpjs.jkmEmployer()));
        tkMap.put("JP", Map.of("rawBase", bpjs.jpRawBase(), "cappedBase", bpjs.jpCappedBase(), "capLimit", bpjs.jpCapLimit(), "employer20Pct", bpjs.jpEmployer(), "employee10Pct", bpjs.jpEmployee()));
        tkMap.put("JKP", Map.of("isEligible", bpjs.jkpEligible(), "employerExtraCost", BigDecimal.ZERO, "employeeExtraDeduction", BigDecimal.ZERO,
                "note", "Status kepesertaan; didanai pemerintah & rekomposisi JKK/JKM tanpa iuran tambahan"));
        bpjsMap.put("ketenagakerjaan", tkMap);
        root.put("bpjs", bpjsMap);
        root.put("pph21", taxTrace);
        root.put("summary", Map.of(
                "grossEarnings", grossEarnings,
                "totalReimbursements", reimbursement,
                "totalDeductions", totalDeductions,
                "netPay", netPay,
                "totalTakeHomeTransfer", totalTakeHome,
                "totalEmployerCost", totalEmployerCost,
                "anomalies", anomalies
        ));
        return root;
    }

    private PayrollDetail buildDetail(UUID resultId, String code, String name, String category, BigDecimal base, BigDecimal rate,
                                      BigDecimal factor, BigDecimal amount, UUID ccId, String formulaDesc) {
        PayrollDetail d = new PayrollDetail();
        d.setId(UUID.randomUUID());
        d.setPayrollResultId(resultId);
        d.setComponentCode(code);
        d.setComponentName(name);
        d.setCategory(category);
        d.setBaseAmount(base.setScale(2, RoundingMode.HALF_UP));
        d.setRate(rate);
        d.setProrateFactor(factor.setScale(6, RoundingMode.HALF_UP));
        d.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        d.setCostCenterId(ccId);
        d.setFormulaDesc(formulaDesc);
        return d;
    }

    private BigDecimal sum(List<PayrollResult> list, Function<PayrollResult, BigDecimal> extractor) {
        return list.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private PayrollRunDto toRunDto(PayrollRun r, PayrollPeriod p) {
        return new PayrollRunDto(
                r.getId(), r.getPayrollPeriodId(),
                p != null ? p.getPeriodName() : "-",
                p != null ? p.getPeriodYear() : (short) 0,
                p != null ? p.getPeriodMonth() : (short) 0,
                p != null && p.isFinalTaxPeriod(),
                r.getRunNumber(), r.getRunType(), r.getStatus(), r.getEngineVersion(),
                r.getTotalEmployees(), r.getTotalGross(), r.getTotalDeductions(), r.getTotalNetPay(), r.getTotalEmployerCost(),
                r.getSnapshotLockedAt(), r.getApprovedBy(), r.getApprovedAt(), r.getCreatedAt()
        );
    }

    private PayrollResultSummaryDto toResultSummary(PayrollResult r) {
        return new PayrollResultSummaryDto(
                r.getId(), r.getPayrollRunId(), r.getEmploymentId(), r.getEmployeeNumber(), r.getEmployeeName(),
                r.getScenarioTitle(), r.getContractType(), r.getPtkpStatus(), r.getTerCategory(), r.getTaxMethod(),
                r.getGrossEarnings(), r.getTotalReimbursements(), r.getBpjsEmployeeTotal(), r.getBpjsEmployerTotal(),
                r.getPph21Amount(), r.getTaxAllowance(), r.getLoanDeduction(), r.getTotalDeductions(),
                r.getNetPay(), r.getTotalTakeHomeTransfer(), r.getTotalEmployerCost(), r.getPaymentStatus(),
                parseJsonList(r.getAnomalyFlagsJson())
        );
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<String> parseJsonList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private Company getActiveCompany() {
        return companyRepo.findFirstByIsActiveTrue()
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "ERR_COMPANY_NOT_FOUND", "Data perusahaan aktif tidak ditemukan."));
    }

    private CompanyPolicyVersion getActivePolicy(UUID companyId) {
        return policyRepo.findFirstByCompanyIdAndStatusOrderByEffectiveFromDesc(companyId, "ACTIVE")
                .orElseThrow(() -> new DomainException(HttpStatus.UNPROCESSABLE_ENTITY, "ERR_PAY_MISSING_POLICY_VERSION", "Versi kebijakan perusahaan aktif tidak ditemukan."));
    }

    private RuleSetVersion getActiveRuleSet() {
        return ruleSetRepo.findFirstByJurisdictionCodeAndStatusOrderByEffectiveFromDesc("ID", "ACTIVE")
                .orElseThrow(() -> new DomainException(HttpStatus.UNPROCESSABLE_ENTITY, "ERR_PAY_MISSING_RULE_VERSION", "Versi aturan regulasi aktif tidak ditemukan."));
    }
}
