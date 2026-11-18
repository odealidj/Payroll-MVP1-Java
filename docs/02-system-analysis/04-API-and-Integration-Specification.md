# API & Integration Specification
## Sistem Informasi Penggajian (Payroll Management System — Standar Indonesia)

**Dokumen ID:** `DOC-SA-API-001`  
**Versi:** 1.1  
**Tanggal:** 18 November 2026 (Revisi Pasca-Audit Internal BA/SA)  
**Acuan:** [01-SRS-Software-Requirements-Specification.md](file:///home/aliube/Workspace/PLan/Payroll/docs/02-system-analysis/01-SRS-Software-Requirements-Specification.md), [02-Use-Case-and-State-Machine.md](file:///home/aliube/Workspace/PLan/Payroll/docs/02-system-analysis/02-Use-Case-and-State-Machine.md), [03-ERD-and-Data-Dictionary.md](file:///home/aliube/Workspace/PLan/Payroll/docs/02-system-analysis/03-ERD-and-Data-Dictionary.md)

---

## 1. Konvensi Protokol, Header & Error Catalog

### 1.1 Standar Protokol
- **Base Path:** `/api/v1`
- **Format:** `application/json; charset=utf-8` (ISO-8601 UTC untuk timestamp `YYYY-MM-DDTHH:mm:ssZ`, `YYYY-MM-DD` untuk tanggal bisnis, string/desimal eksak untuk angka uang agar tidak terjadi pembulatan *floating-point* JSON parser).
- **Paginasi & Filter Standar (Endpoint `GET` Koleksi):** `?page=1&pageSize=50&sort=createdAt:desc&q=...` disertai objek `meta: { page, pageSize, totalItems, totalPages }`.
- **Header Wajib:**
  - `Authorization: Bearer <token>`
  - `X-Company-Id: <uuid>` (Scope entitas perusahaan)
  - `X-Correlation-Id: <uuid>` (Pelacakan distributed tracing & audit log)
  - `X-Idempotency-Key: <uuid/string>` (Wajib untuk seluruh endpoint `POST` transaksi finansial: kalkulasi payroll, approval, pembuatan payment batch, impor mutasi).

### 1.2 Standar Envelope Response

**Success / Error Envelope:**
```json
{
  "success": false,
  "correlationId": "8f21b1d0-4a7b-4f9e-8b3d-9e2a1b4f0c11",
  "data": null,
  "error": {
    "code": "ERR_PAY_RUN_IMMUTABLE",
    "message": "Payroll run telah berstatus APPROVED dan terkunci.",
    "details": [
      { "field": "payrollRunId", "issue": "Gunakan mekanisme Adjustment atau Reversal Run." }
    ]
  }
}
```

### 1.3 Katalog Error Domain Kritis

| Kode Error | HTTP Status | Kondisi Pemicu |
|---|---|---|
| `ERR_PAY_SOD_VIOLATION` | `403` | Checker/Approver mencoba menyetujui tiket yang dibuat oleh dirinya sendiri (*Maker = Approver*). |
| `ERR_PAY_SYSADMIN_DENIED` | `403` | Peran `SYSADMIN` mengakses endpoint data kompensasi, hasil payroll, atau audit log write. |
| `ERR_PAY_IDOR_FORBIDDEN` | `403` | Karyawan mencoba mengakses slip gaji/profil milik `employmentId` lain. |
| `ERR_PAY_DUPLICATE_IDENTITY` | `409` | NIK atau `employeeNumber` sudah terdaftar aktif pada perusahaan. |
| `ERR_PAY_BELOW_MIN_WAGE` | `422` | Total upah tetap di bawah UMP/UMK lokasi kerja pada tanggal efektif. |
| `ERR_PAY_MISSING_RULE_VERSION` | `422` | Tidak ditemukan `RuleSetVersion` aktif untuk tanggal periode payroll. |
| `ERR_PAY_ATTENDANCE_NOT_LOCKED` | `422` | `AttendanceSummary` periode terkait belum berstatus `LOCKED` (cutoff belum selesai). |
| `ERR_PAY_NEGATIVE_NET_PAY` | `422` | Terdapat karyawan dengan `netPay < 0` saat hendak submit approval. |
| `ERR_PAY_RUN_IMMUTABLE` | `409` | Upaya mengubah data pada `PayrollRun` berstatus `APPROVED`/`PAID`/`CLOSED`. |
| `ERR_PAY_IDEMPOTENCY_CONFLICT` | `409` | `X-Idempotency-Key` sama dipakai ulang dengan payload berbeda. |

---

## 2. Katalog Endpoint Lengkap per Domain

| Modul | Method & Endpoint | Peran Diizinkan | Deskripsi Fungsional |
|---|---|---|---|
| **Org & Config** | `GET/POST /api/v1/org/branches`, `/departments`, `/cost-centers` | HR Admin, SysAdmin (struktur non-gaji) | Kelola struktur organisasi & cost center |
| | `GET/POST /api/v1/regulatory-rules` | Tax/BPJS Officer, HR Manager | Daftar & buat versi baru `LegalRuleVersion` (TER, Pasal 17, BPJS, UMP/UMK/UMSP) |
| | `GET/POST /api/v1/company-policies` | HR Manager, Payroll Supv | Kelola versi kebijakan internal perusahaan (`CompanyPolicyVersion`) |
| | `POST /api/v1/regulatory-rules/{id}/simulate` | Tax/BPJS Officer, Payroll Supv | Simulasi uji regresi sebelum aktivasi rule baru |
| **Employee** | `POST /api/v1/employees/onboarding` | HR Admin | Buat profil karyawan, kontrak, struktur gaji awal & submit approval |
| | `GET /api/v1/employees/{id}` | HR Admin, HR Mgr, Payroll, Self | Detail karyawan (NIK/NPWP/rekening otomatis ter-masking sesuai role) |
| | `POST /api/v1/employments/{id}/compensations` | HR Admin | Ajukan perubahan gaji/tunjangan berbasis `validFrom` |
| | `POST /api/v1/employees/{id}/bank-accounts` | Employee, HR Admin | Ajukan rekening baru (memicu verifikasi & check H-5) |
| | `POST /api/v1/opening-balances/ytd` | Tax Officer, Payroll Staff | Impor & validasi saldo awal YTD tengah tahun (`employee_ytd_opening_balances`) |
| **Time & Leave** | `POST /api/v1/attendance/raw-imports` | Attendance Officer, Device Gateway | Terima batch/webhook log absensi mentah tanpa ubah sumber |
| | `POST /api/v1/attendance/corrections` | Employee | Ajukan koreksi exception absensi (approval Line Manager) |
| | `POST /api/v1/attendance/periods/{periodId}/finalize` | Attendance Officer | Validasi exception & kunci `AttendanceSummary` (`LOCKED`) |
| | `GET/POST /api/v1/leaves/requests` | Employee, Line Manager | Pengajuan cuti & pengecekan saldo ledger |
| | `POST /api/v1/overtime/requests` | Line Manager | Buat pra-persetujuan lembur; verifikasi jam aktual & hitung PP 35/2021 |
| **Payroll Run** | `POST /api/v1/payroll-periods` | Payroll Staff | Buka periode payroll beserta tanggal cutoff & payday |
| | `POST /api/v1/payroll-runs` | Payroll Staff | Buat run baru (`REGULAR`, `OFF_CYCLE`, `THR`, `FINAL_SETTLEMENT`, `RETROACTIVE`, `REVERSAL`) |
| | `POST /api/v1/variable-inputs/import` | Payroll Staff | Unggah & validasi batch bonus, insentif & potongan satu kali (`payroll_variable_inputs`) |
| | `POST /api/v1/payroll-runs/{runId}/validate-inputs` | Payroll Staff | Jalankan *Pre-Run Input Validation* & daftar blocker |
| | `POST /api/v1/payroll-runs/{runId}/calculate` | Payroll Staff | Kunci snapshot input/rule & jalankan mesin kalkulasi asinkron |
| | `GET /api/v1/jobs/{jobId}` | Payroll, Finance, Tax | Cek progres & status job asinkron (kalkulasi batch, ekspor laporan besar) |
| | `GET /api/v1/payroll-runs/{runId}/status` | Payroll Staff, Supervisor | Pantau progres batch, jumlah sukses/gagal & ringkasan |
| | `GET /api/v1/payroll-runs/{runId}/results` | Payroll, Tax, BPJS, Finance, Auditor | Daftar hasil *gross-to-net* & *anomaly flags* (paginated) |
| | `GET /api/v1/payroll-runs/{runId}/results/{empId}/trace` | Payroll, Tax, BPJS, Auditor | Detail *Explainable Calculation Trace* (`calculation_trace_json`) |
| | `POST /api/v1/payroll-runs/{runId}/submit-review` | Payroll Staff | Ajukan run ke jalur *Checker-Approver* (tolak jika ada blocker) |
| **Tax & BPJS** | `GET /api/v1/tax/pph21/runs/{runId}/summary` | Tax Officer, Finance | Rekap pemotongan PPh 21, kategori TER, gabungan multi-run & masa terakhir |
| | `GET /api/v1/tax/withholding-slips` | Tax Officer, Employee (self) | Daftar & terbitkan bukti potong PPh 21 tahunan (**BPA1**) / bulanan |
| | `GET /api/v1/tax/pph21/export-ebupot` | Tax Officer | Ekspor format Bukti Potong DJP / Coretax (CSV/XML sesuai spesifikasi Coretax) |
| | `POST /api/v1/bpjs/reconciliations/import` | BPJS Officer | Unggah billing resmi e-Dabu / SIPP & hasilkan exception selisih |
| **THR & Final** | `POST /api/v1/thr-events` | HR Admin, Payroll Staff | Buat event THR, hitung kelayakan & generate `THR` payroll run |
| | `POST /api/v1/terminations` | HR Admin | Catat pengakhiran kerja, hitung kompensasi PKWT / PHK PP 35/2021 |
| | `GET/POST /api/v1/loans` | Employee, Finance | Pengajuan pinjaman, jadwal cicilan & saldo ledger |
| | `GET/POST /api/v1/reimbursements` | Employee, Finance | Pengajuan & persetujuan klaim reimbursement non-upah |
| **Payment & GL** | `POST /api/v1/payment-batches` | Finance Staff | Kelompokkan `PaymentInstruction` run Approved per bank + idempotency |
| | `GET /api/v1/payment-batches/{batchId}/export` | Finance Staff, Finance Mgr | Unduh file upload bank terenkripsi / checksum |
| | `POST /api/v1/payment-batches/{batchId}/reconcile` | Finance Staff | Impor status/mutasi bank -> update `PAID` / `FAILED` |
| | `GET/POST /api/v1/statutory-payments` | Finance, Tax, BPJS Officer | Catat pembayaran kewajiban pajak/BPJS & arsipkan NTPN/bukti VA |
| | `POST /api/v1/journals/generate` | Finance Staff | Generate & validasi keseimbangan jurnal per cost center |
| **Governance** | `POST /api/v1/approvals/{requestId}/decide` | Checker, Approver (berjenjang) | Eksekusi `APPROVE` / `REJECT` dengan validasi SoD ketat |
| | `GET /api/v1/ess/payslips/{resultId}/pdf` | Employee (hanya milik sendiri) | Unduh slip gaji PDF terproteksi password |
| | `GET /api/v1/audit-logs` | Internal Auditor, Compliance | Penelusuran jejak audit *read-only* dengan filter entitas/aktor |

---

## 3. Spesifikasi Payload JSON Kritis

### 3.1 Trigger Kalkulasi Payroll Run (`POST /api/v1/payroll-runs/{runId}/calculate`)

**Request Header:** `X-Idempotency-Key: calc-run-202611-v1`  
**Request Body:**
```json
{
  "lockSnapshot": true,
  "includeRetroactiveQueue": true,
  "notes": "Kalkulasi reguler November 2026 setelah cutoff absensi"
}
```

**Response (`202 Accepted` — Async Batch Job):**
```json
{
  "success": true,
  "correlationId": "a1b2c3d4-1111-2222-3333-444455556666",
  "data": {
    "payrollRunId": "01933e10-8a12-7b00-9100-000000000101",
    "jobId": "job-calc-202611-001",
    "status": "CALCULATING",
    "ruleSetVersionCode": "ID-REG-2026-V1",
    "engineVersion": "1.0.0",
    "totalEligibleEmployees": 1240,
    "snapshotLockedAt": "2026-11-21T03:15:00Z"
  }
}
```

### 3.2 Keputusan Approval Maker-Checker-Approver (`POST /api/v1/approvals/{requestId}/decide`)

**Request Body:**
```json
{
  "decision": "APPROVE",
  "notes": "Total gross-to-net dan rekonsiliasi pajak/BPJS telah sesuai anggaran.",
  "mfaChallengeToken": "totp-verified-891201"
}
```

**Response (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "approvalRequestId": "01933e99-12ab-7000-8811-112233445566",
    "entityType": "PAYROLL_RUN",
    "entityId": "01933e10-8a12-7b00-9100-000000000101",
    "stepCompleted": "CHECKER_FINANCE_MANAGER",
    "nextStep": "APPROVER_DIRECTOR",
    "overallStatus": "CHECKED",
    "decidedAt": "2026-11-22T07:30:10Z"
  }
}
```

### 3.3 Rekonsiliasi Retur Transfer Bank (`POST /api/v1/payment-batches/{batchId}/reconcile`)

**Request Body:**
```json
{
  "bankStatementRef": "STMT-BCA-20261125",
  "items": [
    { "idempotencyKey": "PAY-202611-EMP001-01", "status": "PAID", "bankRefNo": "FT2632900011", "paidAt": "2026-11-25T02:05:00Z" },
    { "idempotencyKey": "PAY-202611-EMP042-01", "status": "FAILED", "failureReason": "ACCOUNT_CLOSED_OR_DORMANT" }
  ]
}
```

**Response (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "batchId": "01934001-0000-7000-0000-000000000001",
    "reconciledCount": 1,
    "failedCount": 1,
    "failedInstructions": [
      {
        "employmentId": "01910000-0000-7000-0000-000000000042",
        "employeeNumber": "EMP042",
        "amount": "8450000.00",
        "nextAction": "UPDATE_AND_VERIFY_BANK_ACCOUNT_THEN_REBATCH"
      }
    ]
  }
}
```

---

## 4. Spesifikasi Kontrak File Integrasi Eksternal

Karena MVP bersifat *tech-agnostic* dan meminimalkan ketergantungan API *Host-to-Host* yang beragam antar-vendor, seluruh integrasi eksternal menggunakan kontrak skema file/payload terstandar berikut:

| Kanal Integrasi | Arah | Format | Kolom / Field Wajib & Aturan Kontrol |
|---|---|---|---|
| **1. Raw Attendance Import** | Masuk -> Sistem | JSON / CSV | `employee_number`, `device_id`, `raw_timestamp` (ISO-8601 + offset), `timezone` (mis. `Asia/Jakarta`), `punch_type` (`IN`/`OUT`). Duplikasi kombinasi `(employee_number, raw_timestamp, device_id)` diabaikan secara idempotent. |
| **2. Bank Payment Batch** | Sistem -> Bank | CSV / Fixed-Width TXT | `source_account`, `beneficiary_bank_code`, `beneficiary_account_no`, `beneficiary_name`, `amount` (integer/tanpa pemisah ribuan sesuai standar bank), `currency` (`IDR`), `remark` (`GAJI NOV 2026`), `customer_ref_no` (`idempotency_key`). Dilengkapi *SHA-256 checksum* file. |
| **3. Bank Response / Mutasi** | Bank -> Sistem | CSV / MT940 | `customer_ref_no` (`idempotency_key`), `bank_ref_no`, `settlement_status` (`SUCCESS`/`FAILED`), `settled_amount`, `error_description`, `value_date`. |
| **4. DJP PPh 21 (BPA1 / Coretax)** | Sistem -> DJP | CSV / XML | `masa_pajak`, `tahun_pajak`, `nik_npwp_penerima`, `nama`, `status_ptkp`, `kode_objek_pajak` (`21-100-01` pegawai tetap; kode lain mengacu skema resmi DJP Coretax terbaru), `penghasilan_bruto`, `pph_dipotong`, `skema_fasilitas`. |
| **5. BPJS Mutasi & Billing Reconcile** | Dua Arah (e-Dabu / SIPP) | Excel / CSV | **Keluar (Mutasi):** `nik`, `no_peserta`, `nama`, `tanggal_efektif`, `upah_dasar`, `jenis_mutasi`. **Masuk (Billing):** `no_peserta`, `nik`, `program_code`, `upah_terdaftar`, `iuran_tagihan_resmi`. |
| **6. Accounting Journal Export** | Sistem -> ERP/Akuntansi | CSV / JSON | `journal_number`, `posting_date`, `company_code`, `cost_center_code`, `coa_account_code`, `account_name`, `debit_amount`, `credit_amount`, `currency`, `line_description`, `payroll_run_ref`. Total Debit wajib sama dengan Total Kredit per `journal_number`. |
