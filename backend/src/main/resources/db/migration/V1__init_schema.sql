-- V1__init_schema.sql
-- Payroll Management System (Standar Indonesia - Modular Monolith)

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Organization & Policy Domain
CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    legal_name VARCHAR(150) NOT NULL,
    npwp_masked VARCHAR(30) NOT NULL,
    jkk_risk_group SMALLINT NOT NULL CHECK (jkk_risk_group BETWEEN 1 AND 5),
    work_pattern VARCHAR(10) NOT NULL CHECK (work_pattern IN ('5_DAYS', '6_DAYS')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (company_id, code)
);

CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (company_id, code)
);

CREATE TABLE positions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    code VARCHAR(30) NOT NULL,
    title VARCHAR(100) NOT NULL,
    default_overtime_eligible BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (company_id, code)
);

CREATE TABLE cost_centers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (company_id, code)
);

CREATE TABLE work_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    branch_id UUID NOT NULL REFERENCES branches(id),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    region_code VARCHAR(20) NOT NULL,
    ump_amount DECIMAL(18,2) NOT NULL,
    timezone VARCHAR(40) NOT NULL DEFAULT 'Asia/Jakarta',
    UNIQUE (company_id, code)
);

CREATE TABLE company_policy_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    version_code VARCHAR(30) NOT NULL UNIQUE,
    default_prorate_method VARCHAR(20) NOT NULL DEFAULT 'WORKING_DAYS',
    working_day_divisor SMALLINT NOT NULL DEFAULT 21,
    variance_warning_pct DECIMAL(5,2) NOT NULL DEFAULT 20.00,
    max_non_statutory_ded_pct DECIMAL(5,2) NOT NULL DEFAULT 50.00,
    bank_change_risk_days SMALLINT NOT NULL DEFAULT 5,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

-- 2. Regulatory Rule Versioning Domain
CREATE TABLE rule_set_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    jurisdiction_code VARCHAR(10) NOT NULL DEFAULT 'ID',
    version_code VARCHAR(30) NOT NULL UNIQUE,
    regulation_ref TEXT NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE tax_ter_brackets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_set_version_id UUID NOT NULL REFERENCES rule_set_versions(id),
    ter_category CHAR(1) NOT NULL CHECK (ter_category IN ('A', 'B', 'C')),
    income_min DECIMAL(18,2) NOT NULL,
    income_max DECIMAL(18,2) NULL,
    rate DECIMAL(10,6) NOT NULL
);
CREATE INDEX idx_tax_ter_lookup ON tax_ter_brackets(rule_set_version_id, ter_category, income_min);

CREATE TABLE tax_pasal17_brackets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_set_version_id UUID NOT NULL REFERENCES rule_set_versions(id),
    layer_no SMALLINT NOT NULL,
    pkp_min DECIMAL(18,2) NOT NULL,
    pkp_max DECIMAL(18,2) NULL,
    rate DECIMAL(10,6) NOT NULL
);

CREATE TABLE bpjs_program_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_set_version_id UUID NOT NULL REFERENCES rule_set_versions(id),
    program_code VARCHAR(20) NOT NULL CHECK (program_code IN ('BPJS_KES', 'JHT', 'JKK', 'JKM', 'JP')),
    risk_group SMALLINT NULL,
    employer_rate DECIMAL(10,6) NOT NULL,
    employee_rate DECIMAL(10,6) NOT NULL,
    min_wage_cap DECIMAL(18,2) NULL,
    max_wage_cap DECIMAL(18,2) NULL,
    max_age_limit SMALLINT NULL
);

CREATE TABLE overtime_multiplier_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_set_version_id UUID NOT NULL REFERENCES rule_set_versions(id),
    work_pattern VARCHAR(10) NOT NULL CHECK (work_pattern IN ('5_DAYS', '6_DAYS')),
    day_category VARCHAR(20) NOT NULL CHECK (day_category IN ('WEEKDAY', 'HOLIDAY', 'SHORT_HOLIDAY')),
    hour_from SMALLINT NOT NULL,
    hour_to SMALLINT NULL,
    multiplier DECIMAL(4,2) NOT NULL
);

-- 3. Employee, Contract, Compensation & YTD Domain
CREATE TABLE employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    full_name VARCHAR(150) NOT NULL,
    birth_date DATE NOT NULL,
    nationality VARCHAR(5) NOT NULL DEFAULT 'ID',
    nik_masked VARCHAR(25) NOT NULL,
    npwp_masked VARCHAR(25) NOT NULL,
    email VARCHAR(150) NOT NULL,
    bank_code VARCHAR(20) NOT NULL,
    bank_account_masked VARCHAR(30) NOT NULL,
    bank_verification_status VARCHAR(20) NOT NULL CHECK (bank_verification_status IN ('PENDING', 'VERIFIED', 'INVALID')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE employments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    employee_id UUID NOT NULL REFERENCES employees(id),
    employee_number VARCHAR(30) NOT NULL,
    scenario_title VARCHAR(150) NOT NULL,
    scenario_description TEXT NOT NULL,
    contract_type VARCHAR(10) NOT NULL CHECK (contract_type IN ('PKWTT', 'PKWT')),
    probation_end_date DATE NULL,
    hire_date DATE NOT NULL,
    contract_end_date DATE NULL,
    termination_date DATE NULL,
    termination_reason VARCHAR(50) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    overtime_eligible BOOLEAN NOT NULL DEFAULT TRUE,
    branch_id UUID NOT NULL REFERENCES branches(id),
    department_id UUID NOT NULL REFERENCES departments(id),
    position_id UUID NOT NULL REFERENCES positions(id),
    work_location_id UUID NOT NULL REFERENCES work_locations(id),
    cost_center_id UUID NOT NULL REFERENCES cost_centers(id),
    ptkp_status VARCHAR(10) NOT NULL CHECK (ptkp_status IN ('TK/0','TK/1','TK/2','TK/3','K/0','K/1','K/2','K/3')),
    ter_category CHAR(1) NOT NULL CHECK (ter_category IN ('A','B','C')),
    tax_method VARCHAR(15) NOT NULL CHECK (tax_method IN ('GROSS','GROSS_UP','NET')),
    bpjs_kes_extra_dependents INT NOT NULL DEFAULT 0,
    is_jkp_eligible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_no_pkwt_probation CHECK (contract_type = 'PKWTT' OR probation_end_date IS NULL),
    UNIQUE (company_id, employee_number)
);

CREATE TABLE salary_components (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(30) NOT NULL CHECK (category IN ('EARNING','DEDUCTION','EMPLOYER_CONTRIBUTION','REIMBURSEMENT','MEMO')),
    is_fixed BOOLEAN NOT NULL,
    is_recurring BOOLEAN NOT NULL,
    tax_treatment VARCHAR(30) NOT NULL CHECK (tax_treatment IN ('TAXABLE_REGULAR','TAXABLE_IRREGULAR','NON_TAXABLE','TAX_ALLOWANCE')),
    is_bpjs_kes_base BOOLEAN NOT NULL DEFAULT FALSE,
    is_bpjs_jht_base BOOLEAN NOT NULL DEFAULT FALSE,
    is_bpjs_jkk_jkm_base BOOLEAN NOT NULL DEFAULT FALSE,
    is_bpjs_jp_base BOOLEAN NOT NULL DEFAULT FALSE,
    is_overtime_base BOOLEAN NOT NULL DEFAULT FALSE,
    is_thr_base BOOLEAN NOT NULL DEFAULT FALSE,
    is_pkwt_comp_base BOOLEAN NOT NULL DEFAULT FALSE,
    prorate_method VARCHAR(20) NOT NULL DEFAULT 'WORKING_DAYS',
    rounding_mode VARCHAR(20) NOT NULL DEFAULT 'HALF_UP_UNIT',
    coa_account_code VARCHAR(50) NOT NULL,
    UNIQUE (company_id, code)
);

CREATE TABLE employee_salary_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    employment_id UUID NOT NULL REFERENCES employments(id),
    salary_component_id UUID NOT NULL REFERENCES salary_components(id),
    amount DECIMAL(18,2) NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NULL,
    change_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_sal_assign_eff ON employee_salary_assignments(employment_id, valid_from, valid_to);

CREATE TABLE employee_ytd_opening_balances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    employment_id UUID NOT NULL REFERENCES employments(id),
    tax_year SMALLINT NOT NULL,
    cutoff_month_count SMALLINT NOT NULL CHECK (cutoff_month_count BETWEEN 1 AND 11),
    ytd_gross_taxable DECIMAL(18,2) NOT NULL DEFAULT 0,
    ytd_biaya_jabatan DECIMAL(18,2) NOT NULL DEFAULT 0,
    ytd_bpjs_worker_pension DECIMAL(18,2) NOT NULL DEFAULT 0,
    ytd_pph21_withheld DECIMAL(18,2) NOT NULL DEFAULT 0,
    UNIQUE (employment_id, tax_year)
);

-- 4. Payroll Period, Attendance, Variable Inputs, Loans & Simulator
CREATE TABLE payroll_periods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    period_year SMALLINT NOT NULL,
    period_month SMALLINT NOT NULL,
    period_name VARCHAR(60) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    cutoff_date DATE NOT NULL,
    pay_date DATE NOT NULL,
    scheduled_working_days INT NOT NULL,
    is_final_tax_period BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(15) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED')),
    UNIQUE (company_id, period_year, period_month)
);

CREATE TABLE attendance_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    payroll_period_id UUID NOT NULL REFERENCES payroll_periods(id),
    employment_id UUID NOT NULL REFERENCES employments(id),
    scheduled_working_days INT NOT NULL,
    actual_working_days INT NOT NULL,
    paid_leave_days DECIMAL(5,2) NOT NULL DEFAULT 0,
    unpaid_leave_days DECIMAL(5,2) NOT NULL DEFAULT 0,
    absent_days DECIMAL(5,2) NOT NULL DEFAULT 0,
    weekday_overtime_hours DECIMAL(6,2) NOT NULL DEFAULT 0,
    holiday_overtime_hours DECIMAL(6,2) NOT NULL DEFAULT 0,
    total_late_minutes INT NOT NULL DEFAULT 0,
    status VARCHAR(15) NOT NULL DEFAULT 'LOCKED' CHECK (status IN ('DRAFT', 'LOCKED')),
    UNIQUE (payroll_period_id, employment_id)
);

CREATE TABLE payroll_variable_inputs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    payroll_period_id UUID NOT NULL REFERENCES payroll_periods(id),
    employment_id UUID NOT NULL REFERENCES employments(id),
    salary_component_id UUID NOT NULL REFERENCES salary_components(id),
    amount DECIMAL(18,2) NOT NULL,
    source_type VARCHAR(20) NOT NULL DEFAULT 'SEED',
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'APPROVED'
);

CREATE TABLE employee_loans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    employment_id UUID NOT NULL REFERENCES employments(id),
    loan_number VARCHAR(40) NOT NULL UNIQUE,
    principal_amount DECIMAL(18,2) NOT NULL,
    monthly_installment DECIMAL(18,2) NOT NULL,
    remaining_balance DECIMAL(18,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REPAYING'
);

CREATE TABLE simulator_overrides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payroll_period_id UUID NOT NULL REFERENCES payroll_periods(id),
    employment_id UUID NOT NULL REFERENCES employments(id),
    tax_method_override VARCHAR(15) NULL CHECK (tax_method_override IN ('GROSS','GROSS_UP','NET')),
    bonus_override DECIMAL(18,2) NULL,
    weekday_overtime_hours_override DECIMAL(6,2) NULL,
    holiday_overtime_hours_override DECIMAL(6,2) NULL,
    unpaid_leave_days_override DECIMAL(5,2) NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (payroll_period_id, employment_id)
);

-- 5. Payroll Execution, Results, Details & Balanced Journals
CREATE TABLE payroll_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    payroll_period_id UUID NOT NULL REFERENCES payroll_periods(id),
    run_number VARCHAR(40) NOT NULL UNIQUE,
    run_type VARCHAR(25) NOT NULL CHECK (run_type IN ('REGULAR', 'THR', 'OFF_CYCLE', 'FINAL_SETTLEMENT')),
    rule_set_version_id UUID NOT NULL REFERENCES rule_set_versions(id),
    company_policy_version_id UUID NOT NULL REFERENCES company_policy_versions(id),
    engine_version VARCHAR(20) NOT NULL DEFAULT '1.1.0',
    status VARCHAR(25) NOT NULL DEFAULT 'DRAFT',
    total_employees INT NOT NULL DEFAULT 0,
    total_gross DECIMAL(18,2) NOT NULL DEFAULT 0,
    total_deductions DECIMAL(18,2) NOT NULL DEFAULT 0,
    total_net_pay DECIMAL(18,2) NOT NULL DEFAULT 0,
    total_employer_cost DECIMAL(18,2) NOT NULL DEFAULT 0,
    snapshot_locked_at TIMESTAMPTZ NULL,
    approved_by VARCHAR(100) NULL,
    approved_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payroll_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    payroll_run_id UUID NOT NULL REFERENCES payroll_runs(id) ON DELETE CASCADE,
    employment_id UUID NOT NULL REFERENCES employments(id),
    cost_center_id UUID NOT NULL REFERENCES cost_centers(id),
    employee_number VARCHAR(30) NOT NULL,
    employee_name VARCHAR(150) NOT NULL,
    scenario_title VARCHAR(150) NOT NULL,
    contract_type VARCHAR(10) NOT NULL,
    ptkp_status VARCHAR(10) NOT NULL,
    ter_category CHAR(1) NOT NULL,
    tax_method VARCHAR(15) NOT NULL,
    gross_earnings DECIMAL(18,2) NOT NULL,
    total_reimbursements DECIMAL(18,2) NOT NULL DEFAULT 0,
    bpjs_employee_total DECIMAL(18,2) NOT NULL,
    bpjs_employer_total DECIMAL(18,2) NOT NULL,
    pph21_amount DECIMAL(18,2) NOT NULL,
    tax_allowance DECIMAL(18,2) NOT NULL DEFAULT 0,
    loan_deduction DECIMAL(18,2) NOT NULL DEFAULT 0,
    total_deductions DECIMAL(18,2) NOT NULL,
    net_pay DECIMAL(18,2) NOT NULL,
    total_take_home_transfer DECIMAL(18,2) NOT NULL,
    total_employer_cost DECIMAL(18,2) NOT NULL,
    payment_status VARCHAR(25) NOT NULL DEFAULT 'PREPARED',
    anomaly_flags_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    calculation_trace_json JSONB NOT NULL,
    UNIQUE (payroll_run_id, employment_id)
);

CREATE TABLE payroll_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payroll_result_id UUID NOT NULL REFERENCES payroll_results(id) ON DELETE CASCADE,
    component_code VARCHAR(30) NOT NULL,
    component_name VARCHAR(120) NOT NULL,
    category VARCHAR(30) NOT NULL,
    base_amount DECIMAL(18,2) NOT NULL,
    rate DECIMAL(10,6) NULL,
    prorate_factor DECIMAL(10,6) NOT NULL DEFAULT 1.000000,
    amount DECIMAL(18,2) NOT NULL,
    cost_center_id UUID NOT NULL REFERENCES cost_centers(id),
    formula_desc TEXT NOT NULL
);

CREATE TABLE journals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    payroll_run_id UUID NOT NULL REFERENCES payroll_runs(id) ON DELETE CASCADE,
    journal_number VARCHAR(50) NOT NULL UNIQUE,
    posting_date DATE NOT NULL,
    total_debit DECIMAL(18,2) NOT NULL,
    total_credit DECIMAL(18,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    CONSTRAINT chk_journal_balanced CHECK (total_debit = total_credit)
);

CREATE TABLE journal_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_id UUID NOT NULL REFERENCES journals(id) ON DELETE CASCADE,
    line_no INT NOT NULL,
    coa_account_code VARCHAR(50) NOT NULL,
    account_name VARCHAR(120) NOT NULL,
    cost_center_id UUID NOT NULL REFERENCES cost_centers(id),
    cost_center_code VARCHAR(30) NOT NULL,
    debit DECIMAL(18,2) NOT NULL DEFAULT 0,
    credit DECIMAL(18,2) NOT NULL DEFAULT 0,
    description TEXT NOT NULL
);
