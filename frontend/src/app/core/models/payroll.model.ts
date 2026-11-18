export interface ApiResponse<T> {
  success: boolean;
  correlationId: string;
  data: T;
  error: {
    code: string;
    message: string;
    details: Array<{ field: string; issue: string }>;
  } | null;
}

export interface PayrollPeriod {
  id: string;
  companyId: string;
  periodYear: number;
  periodMonth: number;
  periodName: string;
  periodStart: string;
  periodEnd: string;
  cutoffDate: string;
  payDate: string;
  scheduledWorkingDays: number;
  finalTaxPeriod: boolean;
  status: string;
}

export interface PayrollRun {
  id: string;
  payrollPeriodId: string;
  periodName: string;
  periodYear: number;
  periodMonth: number;
  finalTaxPeriod: boolean;
  runNumber: string;
  runType: string;
  status: string;
  engineVersion: string;
  totalEmployees: number;
  totalGross: number;
  totalDeductions: number;
  totalNetPay: number;
  totalEmployerCost: number;
  snapshotLockedAt: string;
  approvedBy: string | null;
  approvedAt: string | null;
  createdAt: string;
}

export interface PayrollResultSummary {
  id: string;
  payrollRunId: string;
  employmentId: string;
  employeeNumber: string;
  employeeName: string;
  scenarioTitle: string;
  contractType: string;
  ptkpStatus: string;
  terCategory: string;
  taxMethod: string;
  grossEarnings: number;
  totalReimbursements: number;
  bpjsEmployeeTotal: number;
  bpjsEmployerTotal: number;
  pph21Amount: number;
  taxAllowance: number;
  loanDeduction: number;
  totalDeductions: number;
  netPay: number;
  totalTakeHomeTransfer: number;
  totalEmployerCost: number;
  paymentStatus: string;
  anomalyFlags: string[];
}

export interface PayrollDetailItem {
  id: string;
  componentCode: string;
  componentName: string;
  category: string;
  baseAmount: number;
  rate: number | null;
  prorateFactor: number;
  amount: number;
  formulaDesc: string;
}

export interface PayrollResultDetail {
  summary: PayrollResultSummary;
  items: PayrollDetailItem[];
  calculationTrace: Record<string, unknown>;
}

export interface JournalLine {
  lineNo: number;
  coaAccountCode: string;
  accountName: string;
  costCenterCode: string;
  debit: number;
  credit: number;
  description: string;
}

export interface Journal {
  id: string;
  payrollRunId: string;
  journalNumber: string;
  postingDate: string;
  totalDebit: number;
  totalCredit: number;
  balanced: boolean;
  status: string;
  lines: JournalLine[];
}

export interface EmployeeScenario {
  employmentId: string;
  employeeNumber: string;
  fullName: string;
  scenarioTitle: string;
  scenarioDescription: string;
  contractType: string;
  hireDate: string;
  contractEndDate: string | null;
  terminationDate: string | null;
  ptkpStatus: string;
  terCategory: string;
  taxMethod: string;
  bankCode: string;
  bankAccountMasked: string;
  bankVerificationStatus: string;
  locationName: string;
  umpAmount: number;
  costCenterCode: string;
  fixedMonthlyWage: number;
  bpjsKesExtraDependents: number;
  jkpEligible: boolean;
}

export interface MasterOverview {
  company: {
    id: string;
    code: string;
    legalName: string;
    npwpMasked: string;
    jkkRiskGroup: number;
    workPattern: string;
  };
  activePolicy: {
    versionCode: string;
    defaultProrateMethod: string;
    workingDayDivisor: number;
    varianceWarningPct: number;
    maxNonStatutoryDedPct: number;
  };
  activeRuleSet: {
    id: string;
    versionCode: string;
    regulationRef: string;
    effectiveFrom: string;
  };
  salaryComponents: Array<{
    code: string;
    name: string;
    category: string;
    isFixed: boolean;
    taxTreatment: string;
    bpjsKesBase: boolean;
    overtimeBase: boolean;
    coaAccountCode: string;
  }>;
  bpjsConfigs: Array<{
    programCode: string;
    riskGroup: number | null;
    employerRate: number;
    employeeRate: number;
    maxWageCap: number | null;
    maxAgeLimit: number | null;
  }>;
  pasal17Brackets: Array<{
    layerNo: number;
    pkpMin: number;
    pkpMax: number | null;
    rate: number;
  }>;
  terBracketCount: number;
  employees: EmployeeScenario[];
}

export interface SimulatorOverrideRequest {
  payrollPeriodId: string;
  employmentId: string;
  taxMethodOverride: string | null;
  bonusOverride: number | null;
  weekdayOvertimeHoursOverride: number | null;
  holidayOvertimeHoursOverride: number | null;
  unpaidLeaveDaysOverride: number | null;
}
