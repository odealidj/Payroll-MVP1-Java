# Business Process Analysis (BPA) & Alur Proses Bisnis
## Sistem Informasi Penggajian (Payroll Management System — Standar Indonesia)

**Dokumen ID:** `DOC-BA-BPA-001`  
**Versi:** 1.1  
**Tanggal:** 18 November 2026 (Revisi Pasca-Audit Internal BA/SA)  
**Acuan:** [01-BRD-Business-Requirements-Document.md](file:///home/aliube/Workspace/PLan/Payroll/docs/01-business-analysis/01-BRD-Business-Requirements-Document.md)

---

## 1. Peta Proses Bisnis (Process Landscape)

Arsitektur proses bisnis Sistem Payroll dibagi menjadi 9 proses inti end-to-end yang terhubung secara berurutan dari hulu (manajemen SDM & kehadiran) hingga hilir (pencairan dana, kepatuhan pajak/BPJS, akuntansi, dan offboarding):

```mermaid
flowchart TB
    subgraph HULU["1. Master Data & Time Management"]
        P1["BPA-01: Onboarding\nPKWTT & PKWT"]
        P2["BPA-02: Perubahan Data\n& Kompensasi"]
        P3["BPA-03: Kehadiran, Ledger\nCuti & Lembur"]
    end

    subgraph CORE["2. Core Payroll & Compliance"]
        P4["BPA-04: Siklus Eksekusi\nPayroll Bulanan"]
        P5["BPA-05: Perhitungan & Pelaporan\nPPh Pasal 21"]
        P6["BPA-06: Kepesertaan & Rekonsiliasi\nBPJS Kes & TK"]
        P7["BPA-07: Pengelolaan\nTHR Keagamaan"]
        P8["BPA-08: Offboarding, PKWT\n& Final Settlement"]
    end

    subgraph HILIR["3. Settlement & Accounting"]
        P9["BPA-09: Payment Batch,\nRekonsiliasi Bank & Jurnal"]
    end

    P1 --> P2
    P2 --> P4
    P3 --> P4
    P4 --> P5
    P4 --> P6
    P7 --> P5
    P8 --> P5
    P4 --> P9
    P7 --> P9
    P8 --> P9
```

---

## 2. BPA-01: Onboarding Karyawan Baru (PKWTT & PKWT) dan Aktivasi Struktur Gaji

- **Trigger:** Kandidat menandatangani perjanjian kerja (PKWTT/PKWT) dan dinyatakan siap masuk bekerja.
- **Aktor:** HR Administrator (Maker), Tax Officer & BPJS Officer (Validator), HR Manager (Approver).
- **Input:** Data identitas (KTP/NIK, NPWP), kontrak kerja, surat penempatan, struktur kompensasi, informasi rekening bank.
- **Output:** Record `Employee`, `Employment`, `EmploymentContract`, `EmployeeSalaryAssignment`, `TaxProfile`, `BPJSMembership` berstatus aktif pada tanggal mulai kerja (*effective date*), serta daftar tugas onboarding yang belum selesai (bila ada).

```mermaid
flowchart TD
    A["Mulai: Kandidat diterima"] --> B["HR Admin buat draft Employee & cek duplikasi NIK/NIP"]
    B --> C{"Duplikat\nditemukan?"}
    C -- Ya --> B1["Tampilkan peringatan / tolak draft"]
    C -- Tidak --> D["HR Admin input kontrak (PKWTT/PKWT),\norganisasi, grade, cost center & struktur gaji"]
    D --> E["Tax & BPJS Officer validasi NIK/NPWP,\nPTKP, dan nomor kepesertaan BPJS"]
    D --> F["Verifikasi data rekening bank karyawan"]
    E --> G{"Kelengkapan\ndata wajib?"}
    F --> G
    G -- Belum Lengkap --> G1["Tandai Warning / Hold Payment\n(Onboarding Checklist)"]
    G -- Lengkap --> H["Kirim ke HR Manager untuk Approval"]
    G1 --> H
    H --> I{"Disetujui\nHR Manager?"}
    I -- Ditolak --> D
    I -- Disetujui --> J["Sistem aktifkan Employment pada Effective Date\n& catat Audit Log"]
```

### Gate Kontrol & Exception
- **Hard Block vs Warning:** Karyawan dengan NIK belum valid atau rekening bank belum terverifikasi tetap dapat dibuat dan diaktifkan secara administratif jika sudah bekerja, namun sistem memberi **Warning** pada profil pajak (berisiko tarif lebih tinggi/blokir bukti potong sesuai aturan DJP) dan **Hold Payment** pada instruksi transfer bank (gaji ditahan hingga rekening terverifikasi).

---

## 3. BPA-02: Perubahan Data Karyawan & Kompensasi (Mutasi, Kenaikan Gaji, Rekening Bank)

- **Trigger:** Adanya SK promosi/mutasi organisasi, penyesuaian gaji (awal atau tengah periode), atau permohonan ganti rekening bank.
- **Aktor:** Employee / HR Administrator (Maker), Payroll Supervisor / Finance (Checker), HR Manager / Director (Approver).
- **Prinsip Kunci:** *Append-only bitemporal history* — data lama tidak ditimpa, melainkan ditutup tanggal berakhirnya (`valid_to`) dan dibuat baris versi baru (`valid_from`).

```mermaid
flowchart TD
    A["Trigger: Pengajuan Perubahan\n(Gaji / Jabatan / Cost Center / Rekening)"] --> B{"Jenis Perubahan?"}
    B -- Rekening Bank --> C1["Upload bukti kepemilikan rekening\n& OTP/konfirmasi karyawan"]
    C1 --> C2["Checker: HR/Payroll cek kecocokan nama"]
    C2 --> C3["Approver: HR/Finance Manager"]
    B -- Gaji / Jabatan / Cost Center --> D1["HR Admin buat usulan dengan\nEffective Date & dokumen SK"]
    D1 --> D2["Sistem validasi rentang Grade & UMP/UMK"]
    D2 --> D3["Checker: Payroll Supervisor / HR Manager"]
    D3 --> D4["Approver: Director / Pejabat Berwenang"]
    C3 --> E{"Disetujui?"}
    D4 --> E
    E -- Tidak --> F["Tolak & catat alasan pada Audit Log"]
    E -- Ya --> G["Tutup record lama (valid_to = H-1)\n& buat record baru (valid_from)"]
    G --> H{"Effective Date\n< Periode Berjalan?"}
    H -- Ya --> I["Tandai antrean Retroactive Adjustment (Rapel)"]
    H -- Tidak --> J["Siap dipakai pada Payroll Run berikutnya"]
```

### Gate Kontrol & Exception
- **Perubahan Rekening Mendekati Payday:** Perubahan rekening bank dalam rentang H-5 sebelum hari gajian otomatis memicu status *High-Risk Review* dan mewajibkan verifikasi ganda oleh Finance Manager.
- **Perubahan Gaji Tengah Periode:** Jika `valid_from` berada di tengah periode payroll, engine otomatis memicu kalkulasi multi-segmen (nilai lama untuk hari sebelum `valid_from`, nilai baru sejak `valid_from`).

---

## 4. BPA-03: Pengelolaan Kehadiran, Ledger Cuti, dan Lembur

- **Trigger:** Jadwal shift berjalan, karyawan melakukan absen (clock-in/out), mengajukan cuti/izin, atau ditugaskan lembur.
- **Aktor:** Employee, Line Manager, Attendance Officer, HR Manager.

```mermaid
flowchart TD
    subgraph ABS["A. Kehadiran & Koreksi"]
        A1["Jadwal & Shift ditetapkan"] --> A2["Impor Raw Punch\n(Fingerprint / Mobile / CSV)"]
        A2 --> A3["Simpan Raw Log immutable\n(device, timezone, original timestamp)"]
        A3 --> A4["Auto-matching Punch vs Shift\n(termasuk shift lintas tengah malam)"]
        A4 --> A5{"Ada Exception?\n(Missing/Late/Early)"}
        A5 -- Ya --> A6["Karyawan ajukan koreksi\n-> Line Manager Setujui"]
        A6 --> A7["Attendance Officer Finalisasi"]
        A5 -- Tidak --> A7
    end

    subgraph CUTI["B. Cuti Berbasis Ledger"]
        C1["Karyawan ajukan cuti"] --> C2["Sistem cek saldo ledger,\noverlap & dokumen"]
        C2 --> C3["Line Manager Setujui"]
        C3 --> C4["HR verifikasi jenis cuti\n(Berbayar vs Unpaid)"]
        C4 --> C5["Posting transaksi debit ke Leave Ledger\n& sinkron ke Attendance Summary"]
    end

    subgraph LEMBUR["C. Lembur PP 35/2021"]
        L1["Line Manager buat\nPra-Persetujuan Lembur"] --> L2["Sistem cek kelayakan jabatan\n& batas jam lembur"]
        L2 --> L3["Karyawan jalankan lembur &\nrekam jam aktual"]
        L3 --> L4["Line Manager verifikasi durasi aktual"]
        L4 --> L5["HR validasi kategori hari\n(Hari Kerja / Libur / 5 vs 6 hari)"]
        L5 --> L6["Overtime Engine hitung pengali PP 35/2021"]
    end

    A7 --> SUM["Final Attendance Summary\n(Siap Cutoff Payroll)"]
    C5 --> SUM
    L6 --> SUM
```

### Gate Kontrol & Exception
- **Strict Cutoff:** Data kehadiran, cuti, dan lembur yang disetujui melewati tanggal *cutoff* periode tidak dimasukkan ke run reguler bulan berjalan secara diam-diam, melainkan masuk sebagai penyesuaian (*adjustment*) pada periode berikutnya.
- **Lembur Tanpa Pra-Persetujuan:** Tidak otomatis dibayar kecuali melewati alur *Emergency Overtime Approval* oleh HR Manager.

---

## 5. BPA-04: Siklus Eksekusi Payroll Bulanan (End-to-End 20 Langkah)

- **Trigger:** Awal siklus periode payroll sesuai kalender bulanan perusahaan.
- **Aktor:** Payroll Staff (Maker), Attendance Officer, Payroll Supervisor (Checker), Tax Officer, BPJS Officer, Finance Manager, Director (Approver).

```mermaid
flowchart TD
    S1["1. Buat Payroll Period & Kunci Kalender Cutoff"] --> S2["2. HR Finalisasi Perubahan Master & Kompensasi"]
    S2 --> S3["3. Tutup & Finalisasi Attendance, Leave & Overtime"]
    S3 --> S4["4. Impor Input Variabel (Bonus, Komisi, Potongan, Cicilan)"]
    S4 --> S5["5. Pre-Run Input Validation (Cek Missing Profile / Blocker)"]
    S5 --> S6{"Ada Blocker\nKritis?"}
    S6 -- Ya --> S6B["Selesaikan Exception oleh Pemilik Data"] --> S5
    S6 -- Tidak --> S7["6. Kunci Snapshot Data Input & Rule-Set Version"]
    S7 --> S8["7. Jalankan Payroll Calculation Engine\n(Prorata -> Earning -> PPh 21 -> BPJS -> Deductions -> Net)"]
    S8 --> S9["8. Anomaly Detection & Variance Check\n(Net Pay Negatif, Kenaikan > 20%, dll.)"]
    S9 --> S10["9. Review Silang:\nPayroll Supervisor + Tax Officer + BPJS Officer"]
    S10 --> S11{"Temuan Revisi\nInput?"}
    S11 -- Ya --> S11B["Buka Status ke Draft/Input Validation -> Perbaiki -> Hitung Ulang"] --> S7
    S11 -- Tidak --> S12["10. Finance Manager Cek Anggaran -> Approval Director"]
    S12 --> S13{"Disetujui\nFinal?"}
    S13 -- Ditolak --> S11B
    S13 -- Ya --> S14["11. Payroll Run APPROVED (Immutable / Locked)\nLanjut ke BPA-09 (Payment & Jurnal) & Terbitkan Payslip"]
```

### Gate Kontrol & Exception
- **Snapshot Isolation:** Saat tahap kalkulasi dimulai, sistem membekukan salinan input dan versi aturan (`rule_set_version`) sehingga perubahan master HR yang diinput di tengah proses review tidak mengubah angka run yang sedang ditinjau secara diam-diam.
- **Reopen / Rerun:** Setelah status `Approved`, data tidak dapat diedit. Jika ditemukan kesalahan fatal sebelum dibayar, run harus melalui proses `Reverse` atau `Cancel` dengan otorisasi Director sebelum dibuat run revisi.

---

## 6. BPA-05: Perhitungan, Validasi, dan Pelaporan PPh Pasal 21

- **Trigger:** Eksekusi Payroll Run (Regular, THR, Bonus, atau Final Settlement).
- **Aktor:** Payroll Engine (Otomatis), Tax Officer (Checker/Validator), Finance Staff (Pembayar), Director/Kuasa Pajak (Penandatangan Bukti Potong).

```mermaid
flowchart TD
    A["Input dari Payroll Run:\nBruto Penghasilan + Profil PTKP + Metode Pajak"] --> B{"Cek Masa Pajak?"}
    B -- "Masa Biasa (Jan–Nov)\n& Karyawan Tetap Aktif" --> C1["Tentukan Kategori TER\n(A / B / C) dari PTKP"]
    C1 --> C2["Tambahkan Natura/Premi JKK, JKM,\nBPJS Kes 4% ke Bruto Pajak"]
    C2 --> C3{"Metode Pajak?"}
    C3 -- Gross / Net --> C4["PPh 21 = Bruto * Tarif TER"]
    C3 -- Gross-Up --> C5["Iterasi Tunjangan Pajak hingga\nBruto + Tunjangan Pajak konvergen"]
    
    B -- "Masa Terakhir\n(Desember atau Bulan Berhenti)" --> D1["Akumulasi Bruto Setahun\n(termasuk THR, Bonus, Lembur)"]
    D1 --> D2["Kurangi Biaya Jabatan (5%, max 6jt/thn),\nJHT Pekerja (2%), & JP Pekerja (1%)"]
    D2 --> D3["Penghasilan Neto - PTKP -> Bulatkan ribuan ke bawah = PKP"]
    D3 --> D4["Hitung Pajak Setahun\ndengan Tarif Progresif Pasal 17 UU HPP"]
    D4 --> D5["PPh Masa Terakhir = Pajak Setahun\n- Total PPh yang Telah Dipotong Jan–Nov"]
    D5 --> D6{"Hasil Kurang / Lebih\nPotong?"}
    D6 -- Kurang Potong (+) --> D7["Potong dari Gaji Bulan Terakhir"]
    D6 -- Lebih Potong (-) --> D8["Kembalikan Kelebihan Potong ke Karyawan (Refund)"]

    C4 --> E["Simpan Tax Trace Detail"]
    C5 --> E
    D7 --> E
    D8 --> E
    E --> F["Tax Officer Review & Rekonsiliasi Ledger -> Ekspor Bukti Potong & Pelaporan DJP"]
```

---

## 7. BPA-06: Kepesertaan, Perhitungan, dan Rekonsiliasi BPJS (Kesehatan & Ketenagakerjaan)

- **Trigger:** Onboarding/offboarding karyawan, perubahan upah tetap, dan eksekusi payroll bulanan.
- **Aktor:** HR Admin, BPJS Officer, Payroll Engine, Finance Staff.

```mermaid
flowchart TD
    A["Data Karyawan Aktif & Perubahan Upah Tetap"] --> B["BPJS Officer kelola mutasi masuk/keluar\n& validasi nomor peserta (e-Dabu / SIPP)"]
    B --> C["Payroll Engine hitung terpisah:\n1) BPJS Kes (4% + 1% + Tanggungan)\n2) JHT (3.7% + 2%)\n3) JKK (0.24%–1.74%)\n4) JKM (0.30%)\n5) JP (2% + 1%, Batas Atas)\n6) JKP (Rekomposisi)"]
    C --> D["Sistem hasilkan Rekap Iuran Pekerja (Potongan)\n& Beban Perusahaan (Employer Cost)"]
    D --> E["Impor Tagihan / Rekap Resmi\nBPJS Kesehatan & BPJS TK"]
    E --> F["Auto-Reconcile per Nomor Peserta"]
    F --> G{"Ada Selisih?"}
    G -- Tidak --> H["Finance Bayar & Arsipkan Bukti Bayar"]
    G -- Ya --> I["Masuk Exception Queue:\n(Beda Upah / Mutasi Terlambat / Ganda)"]
    I --> J["BPJS Officer & HR investigasi\n(DILARANG ubah hasil payroll paksa)"]
    J --> K["Buat Adjustment pada periode berikut atau bayar tagihan terkoreksi -> Selesai"]
```

---

## 8. BPA-07: Pengelolaan dan Pembayaran THR Keagamaan

- **Trigger:** HR membuat *THR Event* sesuai kalender Hari Raya Keagamaan (atau serentak menjelang Idul Fitri sesuai kebijakan perusahaan) minimal H-21 sebelum Hari Raya.
- **Aktor:** HR Admin, Payroll Staff, Tax Officer, Finance Manager, Director.

```mermaid
flowchart TD
    A["H-21: HR buat THR Event\n(Tentukan Tanggal Hari Raya Acuan & Tanggal Bayar <= H-7)"] --> B["Engine tarik daftar karyawan aktif\n& cek tanggal masuk serta status kontrak"]
    B --> C["Filter kelayakan: Masa kerja >= 1 bulan terus-menerus\n(termasuk cek aturan resign/PHK PKWTT H-30)"]
    C --> D["Hitung nominal THR:\n>= 12 bln: 100% Upah Tetap\n1–<12 bln: (Masa Kerja/12) * Upah Tetap"]
    D --> E["Kirim bruto THR ke Tax Engine PPh 21\n(digabung dengan masa pajak bulan bayar)"]
    E --> F["Payroll Supervisor & Tax Officer Review"]
    F --> G["Finance & Director Approval"]
    G --> H["Generate Payment Batch <= H-7 sebelum Hari Raya"]
    H --> I["Terbitkan Slip THR & Rekam Jurnal Akuntansi"]
```

---

## 9. BPA-08: Offboarding, Final Settlement, Kompensasi PKWT & PHK

- **Trigger:** Pengajuan pengunduran diri (*resign*), berakhirnya masa kontrak PKWT, pensiun, atau pemutusan hubungan kerja (PHK).
- **Aktor:** HR Admin, Legal/HR Manager, Attendance Officer, Payroll Staff, Finance Manager, Director.

```mermaid
flowchart TD
    A["HR catat Termination Event\n(Alasan Berhenti & Tanggal Efektif Terakhir)"] --> B["HR/Legal validasi jenis hak:\nResign / Habis PKWT / PHK / Pensiun"]
    B --> C["Hentikan accrual cuti & komponen gaji\nper tanggal efektif berhenti"]
    C --> D["Finalisasi absensi & lembur sampai hari kerja terakhir"]
    D --> E["Hitung Prorata Gaji Bulan Terakhir"]
    E --> F{"Jenis Pengakhiran?"}
    F -- "PKWT Berakhir\n(Masa >= 1 bln)" --> G1["Hitung Uang Kompensasi PKWT\n(Masa Kerja/12 * 1 Bln Upah)"]
    F -- "PHK" --> G2["Tentukan Pengali UP, UPMK & UPH\ndari Decision Table PP 35/2021 + Sign-Off Legal"]
    F -- "Resign Sukarela" --> G3["Hitung Uang Pisah (jika ada di PP/PKB)\n& Kompensasi Cuti yang belum diambil"]
    G1 --> H["Cek & potong pelunasan sisa Pinjaman / Kewajiban Sah"]
    G2 --> H
    G3 --> H
    H --> I["Tax Engine hitung PPh 21 Masa Terakhir (Pasal 17)\n& Pajak Pesangon Final (PP 68/2009 bila ada)"]
    I --> J["Review Payroll/HR -> Approval Director -> Payment Batch Final & Bukti Potong BPA1"]
```

---

## 10. BPA-09: Pembuatan Payment Batch Bank, Rekonsiliasi, dan Jurnal Akuntansi

- **Trigger:** Payroll Run / THR Run / Final Run mencapai status `Approved`.
- **Aktor:** Finance Staff (Maker), Finance Manager (Checker/Approver), Authorized Bank Signatory.

```mermaid
flowchart TD
    A["Payroll Run Approved"] --> B["Sistem validasi status rekening\n& nama pemilik rekening"]
    B --> C{"Ada Rekening\nInvalid / Pending?"}
    C -- Ya --> C1["Hold Payment Instruction karyawan terkait\n(Masuk Exception Rekening)"]
    C -- Tidak --> D["Finance Staff (Maker) kelompokkan batch\nberdasarkan Bank & Tanggal Transfer"]
    C1 --> D
    D --> E["Generate Bank File / Payment Instruction\ndengan Idempotency Key unik"]
    E --> F["Checker: Finance Manager cocokkan\nTotal Batch == Total Approved Net Pay (minus Hold)"]
    F --> G["Authorized Approver rilis pembayaran di kanal Bank"]
    G --> H["Impor File Status Bank / Rekening Koran"]
    H --> I{"Semua transaksi\nberhasil?"}
    I -- Ada Gagal/Retur --> I1["Tandai 'Failed' -> Koreksi Rekening\n-> Proses Bayar Ulang (Re-batch)"]
    I -- Berhasil --> J["Status Penerima = 'Paid' & 'Reconciled'"]
    I1 --> J
    J --> K["Auto-Generate & Post Jurnal Akuntansi\n(Beban Gaji, Utang Net Pay, PPh 21, BPJS per Cost Center)"]
    K --> L["Tutup Periode Payroll & Arsipkan Snapshot"]
```

---

## 11. BPA-10: Siklus Penutupan Akhir Tahun (Desember), Penerbitan BPA1 & Rollover Awal Tahun (Januari)

- **Trigger:** Memasuki masa payroll Desember (penutupan tahun pajak) dan tanggal 1 Januari (pembukaan tahun baru).
- **Aktor:** Payroll Staff, Tax Officer, HR Admin, HR Manager, Finance Manager, Director.

```mermaid
flowchart TD
    subgraph DES["A. Penutupan Akhir Tahun (Desember)"]
        D1["1. Jalankan Payroll Desember (Masa Pajak Terakhir)"] --> D2["2. Engine Rekonsiliasi Pasal 17 Setahun\n(Gabung Bruto Semua Run + YTD Opening Balance)"]
        D2 --> D3["3. Tangani Kurang Potong & Pengembalian Lebih Potong (Refund)"]
        D3 --> D4["4. Tax Officer Validasi & Terbitkan Bukti Potong Tahunan BPA1 (Coretax)"]
        D4 --> D5["5. HR Hitung Carry-Forward & Hangus (Expiry) Cuti di Leave Ledger"]
    end

    subgraph JAN["B. Rollover Awal Tahun (Januari)"]
        J1["6. Aktivasi CompanyPolicyVersion & LegalRuleVersion Baru\n(UMP/UMK/UMSP Tahun Baru, Batas JP, Kalender Libur Nasional)"] --> J2["7. Terapkan Perubahan PTKP Baru (per 1 Januari) pada TaxProfile"]
        J2 --> J3["8. Reset Akumulator Pajak YTD ke 0 & Posting Accrual Cuti Tahun Baru"]
        J3 --> J4["9. Siap Siklus Payroll Januari"]
    end

    D5 --> J1
```
