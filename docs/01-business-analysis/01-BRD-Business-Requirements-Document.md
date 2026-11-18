# Business Requirements Document (BRD)
## Sistem Informasi Penggajian (Payroll Management System — Standar Indonesia)

**Dokumen ID:** `DOC-BA-BRD-001`  
**Versi:** 1.1  
**Tanggal:** 18 November 2026 (Revisi Pasca-Audit Internal BA/SA)  
**Status:** Siap untuk Validasi Stakeholder (HR, Finance, Tax, Legal, Manajemen)  
**Acuan Utama:** [Payroll_Business_Process_Analysis_ID.md](file:///home/aliube/Workspace/PLan/Payroll/Payroll_Business_Process_Analysis_ID.md) & [00-Master-Roadmap-BA-SA-SDLC.md](file:///home/aliube/Workspace/PLan/Payroll/docs/00-Master-Roadmap-BA-SA-SDLC.md)

---

## 1. Executive Summary

Perusahaan membutuhkan **Sistem Informasi Penggajian (Payroll Management System)** yang bukan sekadar alat kalkulasi spreadsheet otomatis, melainkan sistem transaksi finansial dan kepatuhan (*compliance*) yang akurat, transparan, dan dapat diaudit sesuai regulasi ketenagakerjaan serta perpajakan di Indonesia.

Untuk memastikan implementasi yang terukur dan tepat sasaran, fase pertama (**MVP / Fase 1**) difokuskan pada **Single-Company (1 Badan Hukum PT dengan dukungan multi-cabang & multi-cost center)** untuk pengelolaan **karyawan bulanan tetap (PKWTT) dan karyawan kontrak (PKWT)**. Meskipun MVP berjalan untuk satu badan hukum, struktur data disiapkan dengan atribut entitas perusahaan agar siap berkembang ke skema multi-company dan pekerja harian/borongan pada fase berikutnya.

### 1.1 Masalah Bisnis yang Diselesaikan
1. **Risiko Kepatuhan & Salah Hitung**: Perhitungan manual PPh 21 (TER PP 58/2023 & rekonsiliasi masa pajak terakhir PMK 168/2023), 5 program BPJS Ketenagakerjaan, BPJS Kesehatan, lembur (PP 35/2021), THR (Permenaker 6/2016), dan uang kompensasi PKWT rentan salah hitung dan sanksi denda.
2. **Kurangnya Kontrol Internal (*Separation of Duties*)**: Spreadsheet tidak memiliki mekanisme *Maker–Checker–Approver* yang ketat sehingga rawan manipulasi data gaji, rekening bank, maupun perubahan nominal setelah disetujui.
3. **Absennya Reproduksibilitas Historis**: Perubahan tarif regulasi atau kenaikan gaji sering menimpa histori lama sehingga angka payroll periode terdahulu tidak dapat direproduksi ulang untuk keperluan audit atau pemeriksaan pajak.
4. **Proses Rekonsiliasi Lambat**: Pencocokan antara data absensi, pengajuan lembur/cuti, hasil transfer bank, tagihan BPJS, dan jurnal akuntansi masih terfragmentasi.

### 1.2 Tujuan Bisnis & KPI Keberhasilan

| Kode | Tujuan Bisnis | Indikator Keberhasilan (KPI / Target) |
|---|---|---|
| **OBJ-01** | Akurasi & Kepatuhan Regulasi | **100% kesesuaian** hasil uji paralel (*parallel run*) terhadap formula resmi PPh 21, BPJS, Lembur PP 35/2021, THR, dan kompensasi PKWT (0 selisih tak terjelaskan). |
| **OBJ-02** | Efisiensi Siklus Payroll | Waktu penyusunan hingga persetujuan payroll bulanan (100–5.000 karyawan) berkurang minimal **60%**, dan proses kalkulasi batch 1.000 karyawan selesai dalam **< 5 menit**. |
| **OBJ-03** | Integritas Audit & Traceability | **100% transaksi** memiliki *calculation trace* (formula, basis, tarif, `rule_set_version`) dan *immutable audit log* yang memisahkan pembuat, pemeriksa, penyetuju, dan pembayar. |
| **OBJ-04** | Kecepatan Rekonsiliasi Finansial | Rekonsiliasi status transfer bank, tagihan BPJS, PPh 21, dan jurnal akuntansi per cost center selesai dalam **H+2 hari kerja** setelah tanggal bayar. |

---

## 2. Ruang Lingkup Proyek (Scope Management)

### 2.1 Dalam Ruang Lingkup MVP (Fase 1)
1. **Struktur Organisasi (Single-Company, Multi-Branch)**: Pengelolaan 1 Legal Entity (PT), cabang/lokasi kerja, divisi/unit bisnis, departemen, jabatan, *job grade*, *cost center*, dan *reporting line*.
2. **Master Karyawan & Hubungan Kerja**: Profil karyawan PKWTT dan PKWT, identitas (NIK/NPWP), kontak, dokumen kontrak, rekening bank terverifikasi, histori penugasan, dan riwayat mutasi/promosi berbasis *effective date*.
3. **Manajemen Kompensasi**: Komponen penghasilan & potongan multi-dimensi (memisahkan perlakuan pajak, BPJS per program, dasar lembur, dasar THR, dan akun COA), struktur gaji per grade, serta prorata dan *multi-segment split* untuk perubahan gaji tengah periode.
4. **Waktu Kerja, Absensi, Cuti & Lembur**: Kalender kerja (pola 5 hari dan 6 hari kerja), manajemen shift (termasuk shift lintas tengah malam), impor raw log absensi tanpa mengubah sumber, penanganan exception absensi, ledger saldo cuti, serta pengajuan & kalkulasi lembur berbasis pra-persetujuan.
5. **Siklus Payroll Run**: Regular monthly payroll, Off-cycle payroll, THR run, Bonus/Incentive run, Final payroll (resign/habis kontrak/PHK), Retroactive adjustment (rapel), dan Reversal terotorisasi.
6. **Kepatuhan Regulasi Indonesia**:
   - **PPh Pasal 21**: Profil PTKP, kategori TER (A/B/C), pemotongan masa biasa (Jan–Nov), rekonsiliasi Pasal 17 pada masa pajak terakhir (Desember/berhenti), dukungan skema **Gross**, **Gross-Up**, dan **Net**, serta output bukti potong & data pelaporan DJP/Coretax.
   - **BPJS Kesehatan**: Kepesertaan PPU (4% pemberi kerja, 1% pekerja + tanggungan tambahan), batas upah min/max berversi, dan rekonsiliasi tagihan.
   - **BPJS Ketenagakerjaan**: Kalkulasi terpisah 5 program (**JHT, JKK sesuai kelas risiko, JKM, JP dengan batas atas upah, JKP**) serta rekonsiliasi tagihan.
   - **THR Keagamaan & Kompensasi PKWT / PHK**: Perhitungan proporsional & penuh sesuai Permenaker 6/2016 dan PP 35/2021.
7. **Pinjaman, Potongan & Reimbursement**: Pengajuan & jadwal cicilan pinjaman karyawan, potongan sah terotorisasi dengan kontrol batas maksimum, dan reimbursement yang terpisah dari basis upah/pajak.
8. **Pembayaran, Akuntansi & Pelaporan**: Pembuatan *payment batch file* standar bank Indonesia, pelacakan status retur/gagal transfer, jurnal akuntansi (pengakuan beban, utang gaji/pajak/BPJS, pembayaran, alokasi *cost center*), penerbitan slip gaji PDF, serta Employee Self-Service (ESS) dasar (lihat slip, pengajuan cuti/lembur/koreksi absensi).
9. **Tata Kelola & Keamanan**: RBAC berbasiskan peran dan cakupan cabang, *Maker–Checker–Approver*, *regulatory configuration versioning*, dan *immutable audit trail*.

### 2.2 Di Luar Ruang Lingkup MVP (Masuk Fase 2 / Lanjutan)
1. **Multi-Company Intercompany Settlement**: Pemrosesan lintas beberapa badan hukum (PT berbeda) dalam satu proses konsolidasi dan jurnal antar-entitas (atribut `company_id` tetap disediakan pada skema data MVP untuk kesiapan ekspansi).
2. **Pekerja Harian Lepas (BHL), Borongan & Siklus Mingguan/Harian**: Skema upah satuan hasil atau siklus payroll non-bulanan.
3. **Integrasi API Bank Host-to-Host Real-Time & ERP Dua Arah**: MVP menggunakan mekanisme ekspor/impor file terstruktur dan webhook/batch yang dapat ditingkatkan ke API H2H pada Fase 2.
4. **Modul HR Non-Payroll Penuh**: Rekrutmen (ATS), Performance Management (KPI/OKR), Talent & Succession Planning, Learning Management (LMS), dan General Ledger penuh.
5. **Komisi Berjenjang Kompleks & Shift Roster Otomatis Industri Khusus**: Skema komisi sales multi-tier dari POS/CRM atau optimasi jadwal roster berbasis AI.

---

## 3. Pemangku Kepentingan, Peran Aktor & Pemisahan Tugas (SoD)

### 3.1 Profil Aktor & Tanggung Jawab Utama

| ID Aktor | Peran (Role) | Tanggung Jawab Utama dalam Sistem | Pembatasan Akses Kritis |
|---|---|---|---|
| **ACT-01** | **Employee** | Melihat slip gaji & bukti potong pribadi, memperbarui data kontak terbatas, mengajukan cuti, izin, koreksi absensi, lembur (aktual), dan pinjaman. | Hanya dapat membaca/mengubah data milik sendiri (proteksi anti-IDOR); tidak bisa melihat gaji karyawan lain. |
| **ACT-02** | **Line Manager** | Memeriksa dan menyetujui/menolak cuti, pengajuan lembur, dan koreksi absensi anggota tim di bawah garis pelaporannya. | Terbatas hanya pada bawahan langsung/tidak langsung sesuai struktur organisasi. |
| **ACT-03** | **HR Administrator** | Memelihara data master karyawan, kontrak PKWTT/PKWT, mutasi, promosi, usulan struktur gaji, dan proses onboarding/offboarding. | Tidak dapat menyetujui perubahan gaji/rekening yang dibuatnya sendiri dan tidak dapat melakukan disbursement bank. |
| **ACT-04** | **HR Manager** | Menyetujui onboarding, mutasi, perubahan kompensasi, pengakhiran hubungan kerja (resign/PHK), dan kebijakan cuti/HR internal. | Tidak dapat membuat batch pembayaran bank atau mengubah hasil kalkulasi payroll yang sudah dikunci. |
| **ACT-05** | **Attendance Officer** | Mengelola kalender kerja, shift, impor raw absensi, penyelesaian exception absensi, dan finalisasi ringkasan kehadiran sebelum cutoff. | Tidak memiliki akses terhadap nominal gaji atau data pajak/rekening karyawan. |
| **ACT-06** | **Payroll Staff** | Membuka periode payroll, memverifikasi kelengkapan input variabel, membuat snapshot, dan menjalankan engine kalkulasi payroll/THR/final. | Berperan sebagai **Maker**; tidak boleh menyetujui payroll run yang dijalankannya atau merilis pembayaran bank. |
| **ACT-07** | **Payroll Supervisor** | Memeriksa hasil kalkulasi, menindaklanjuti laporan *variance* dan *anomaly*, memvalidasi keakuratan gross-to-net sebelum persetujuan final. | Berperan sebagai **Checker**; tidak boleh memodifikasi angka hasil secara manual tanpa mekanisme input/koreksi resmi. |
| **ACT-08** | **Tax Officer** | Memvalidasi NIK/NPWP dan status PTKP/TER, memeriksa tax trace PPh 21, melakukan rekonsiliasi masa terakhir, dan mengunduh data pelaporan DJP. | Fokus pada validasi kepatuhan pajak; tidak dapat memodifikasi komponen gaji pokok. |
| **ACT-09** | **BPJS Officer** | Memelihara nomor kepesertaan, kelas risiko JKK, mutasi peserta masuk/keluar, dan merekonsiliasi perhitungan internal terhadap tagihan SIPP/e-Dabu. | Tidak dapat memaksa perubahan angka payroll untuk mencocokkan tagihan (selisih wajib lewat alur exception/koreksi). |
| **ACT-10** | **Finance Staff** | Memeriksa ketersediaan dana, menyusun *payment batch file* sesuai rekening terverifikasi, menangani retur transfer, dan menyusun draf jurnal akuntansi. | Tidak dapat mengubah angka hasil payroll; hanya membaca instruksi pembayaran dari run berstatus `Approved`. |
| **ACT-11** | **Finance Manager** | Memeriksa total nominal payroll terhadap anggaran, menyetujui *payment batch*, memvalidasi rekonsiliasi bank, dan menyetujui posting jurnal. | Wajib berbeda individu dari Finance Staff (*Maker*) yang menyiapkan file transfer. |
| **ACT-12** | **Director / Authorized Approver** | Memberikan otorisasi final (*final sign-off*) atas Payroll Run, THR Run, Final Payroll, serta perubahan konfigurasi regulasi/kebijakan gaji tingkat perusahaan. | Terpisah dari proses input data dan eksekusi harian. |
| **ACT-13** | **System Administrator** | Mengelola user account, konfigurasi teknis, integrasi, backup, dan monitoring kesehatan sistem. | **Dilarang keras** memiliki akses baca/ubah ke nominal gaji, hasil payroll, atau menghapus/mengubah *audit log*. |
| **ACT-14** | **Internal Auditor** | Melakukan peninjauan *read-only* terhadap master histori, calculation trace, rekonsiliasi bank/pajak/BPJS, dan *audit log*. | Akses mutlak *read-only*; tidak dapat membuat, menyetujui, atau menghapus transaksi apa pun. |

### 3.2 Matriks Pemisahan Tugas (Separation of Duties — Maker / Checker / Approver)

| Transaksi Kritis | Maker (Pembuat) | Checker (Pemeriksa) | Approver (Penyetuju Final) | Executor (Pelaksana Sistem/Dana) |
|---|---|---|---|---|
| **Onboarding & Data Kontrak** | HR Administrator | HR Supervisor / Tax & BPJS Officer (validasi ID) | HR Manager | Sistem (Aktivasi pada tanggal mulai) |
| **Perubahan Gaji / Kompensasi** | HR Administrator | Payroll Supervisor / HR Manager | Director / Authorized Approver | Payroll Engine (sesuai *effective date*) |
| **Perubahan Rekening Bank** | Employee / HR Admin | HR Admin / Payroll Staff (verifikasi buku tabungan) | HR Manager / Finance Manager | Sistem (Update terverifikasi) |
| **Lembur & Cuti** | Employee / Line Manager | Line Manager & Attendance Officer | HR Manager (untuk jenis khusus) | Attendance Summary -> Payroll Input |
| **Payroll Run / THR / Final Run** | Payroll Staff | Payroll Supervisor + Tax Officer + BPJS Officer | Finance Manager & Director | Sistem (Lock Run & Terbitkan Payslip) |
| **Payment Batch & Pencairan Bank** | Finance Staff | Finance Manager / Payroll Supervisor | Authorized Bank Signatory (Director/Finance) | Bank Channel + Finance Rekonsiliasi |
| **Koreksi / Reversal Payroll Approved** | Payroll Staff (dengan alasan tertulis) | Payroll Supervisor & Finance Manager | Director / Authorized Approver | Sistem (Create Adjustment/Reversal Run) |
| **Perubahan Konfigurasi Regulasi** | Tax Officer / BPJS Officer / HR | Payroll Supervisor & Legal | HR Manager & Finance Manager / Director | Sistem (Simpan versi baru ber-effective date) |

### 3.3 Mode Fleksibel SoD untuk Organisasi Ramping (Single-Company MVP)
Keempat belas aktor di atas merupakan **peran logis (*logical roles*)**, bukan kewajiban memiliki 14 orang berbeda. Untuk perusahaan skala kecil/menengah (*single-company*):
- Satu akun pengguna diperbolehkan memegang lebih dari satu peran logis (misalnya HR Manager merangkap BPJS Officer, atau Finance Manager merangkap Approver pembayaran tertentu).
- **Aturan Keras (*Hard System Guard*) tetap mutlak berlaku pada level transaksi:** Pada satu tiket/dokumen transaksi yang sama, pengguna yang bertindak sebagai **Maker tidak boleh bertindak sebagai Checker maupun Approver** (`checker_id != maker_id` dan `approver_id != maker_id`).

---

## 4. Katalog Kebutuhan Bisnis (Business Requirements)

Setiap kebutuhan bisnis diberi ID unik `BR-xxx`, prioritas MoSCoW (**Must**, **Should**, **Could**), dan kriteria keberhasilan terukur untuk diturunkan menjadi SRS dan Test Case.

### 4.1 Domain Organisasi, Karyawan & Kompensasi

| ID | Kebutuhan Bisnis | Prioritas | Kriteria Keberhasilan / Acceptance Criteria |
|---|---|---|---|
| **BR-001** | **Struktur Organisasi Hierarkis & Cost Center** | Must | Sistem mengelola 1 badan hukum (PT) beserta cabang, departemen, jabatan, *job grade*, lokasi kerja (terkait UMK/UMP), kalender kerja, dan *cost center* dengan histori tanggal efektif. |
| **BR-002** | **Manajemen Master Karyawan & Kontrak PKWTT/PKWT** | Must | Sistem menyimpan identitas karyawan (dengan pencegahan duplikasi NIK/Employee ID), jenis perjanjian (PKWTT/PKWT), masa berlaku kontrak, status aktif/resign/PHK, dan dokumen pendukung dengan kontrol akses & enkripsi data pribadi. |
| **BR-003** | **Histori Bitemporal & Effective Dating** | Must | Setiap perubahan jabatan, lokasi, cost center, rekening bank, atau gaji tidak menimpa data lama; sistem menyimpan nilai sebelum-sesudah, `valid_from/valid_to`, alasan perubahan, dan jejak *Maker-Checker-Approver*. |
| **BR-004** | **Klasifikasi Komponen Gaji Multi-Dimensi** | Must | Setiap komponen gaji memiliki atribut independen untuk: kategori (Earning/Deduction/Employer/Reimbursement/Memo), sifat tetap/variabel, dasar PPh 21, dasar BPJS Kesehatan, dasar per program BPJS TK, dasar lembur, dasar THR, prorata, pembulatan, dan mapping COA. |
| **BR-005** | **Struktur Gaji, Prorata & Multi-Segment Tengah Periode** | Must | Perubahan kompensasi di tengah periode payroll otomatis memecah periode menjadi beberapa segmen tanggal efektif dan menghitung nilai proporsional per segmen berdasarkan konfigurasi hari kerja terjadwal atau hari kalender. |

### 4.2 Domain Waktu Kerja, Absensi, Cuti & Lembur

| ID | Kebutuhan Bisnis | Prioritas | Kriteria Keberhasilan / Acceptance Criteria |
|---|---|---|---|
| **BR-006** | **Impor Raw Attendance & Pelestarian Sumber** | Must | Sistem mengimpor data log absensi (fingerprint/mobile/file batch), menyimpan timestamp asli beserta zona waktu dan ID perangkat tanpa modifikasi, serta memadankannya dengan shift (termasuk shift lintas tengah malam). |
| **BR-007** | **Manajemen Exception & Cutoff Kehadiran** | Must | Sistem mendeteksi anomali (missing punch, terlambat, pulang cepat, tanpa keterangan), menyediakan alur koreksi berpersetujuan, dan mewajibkan finalisasi ringkasan kehadiran sebelum kalkulasi payroll. |
| **BR-008** | **Manajemen Cuti Berbasis Ledger** | Must | Saldo cuti dikelola melalui buku besar transaksi (`opening`, `accrual`, `usage`, `adjustment`, `carry_forward`, `expiry`), mendukung validasi overlap jadwal/saldo, serta membedakan cuti berbayar vs *unpaid leave* ke payroll. |
| **BR-009** | **Pengajuan & Perhitungan Lembur Sesuai PP 35/2021** | Must | Hanya lembur yang memiliki pra-persetujuan (atau izin khusus terotorisasi) dan verifikasi aktual yang masuk payroll; perhitungan menggunakan basis upah/173 serta tabel pengali dinamis sesuai jenis hari dan pola 5/6 hari kerja. |

### 4.3 Domain Payroll Execution Engine

| ID | Kebutuhan Bisnis | Prioritas | Kriteria Keberhasilan / Acceptance Criteria |
|---|---|---|---|
| **BR-010** | **Siklus Periode & Berbagai Jenis Payroll Run** | Must | Sistem mendukung periode bulanan beserta jenis run: Regular, Off-Cycle, THR, Bonus/Insentif, Final Payroll, Retroactive Adjustment (Rapel), dan Reversal. |
| **BR-011** | **Mesin Kalkulasi Deterministik & Explainable Trace** | Must | Setiap run menyimpan *snapshot* data input dan *rule-set version*; menjalankan ulang run dengan snapshot sama menghasilkan angka identik, serta menampilkan rincian rumus, basis, pembulatan, dan histori kalkulasi per karyawan. |
| **BR-012** | **Imutabilitas Run & Mekanisme Koreksi Terkendali** | Must | Run berstatus `Approved` atau `Paid` terkunci penuh; perubahan hanya dapat dilakukan melalui mekanisme *adjustment* periode berikut, *off-cycle*, atau *reversal* dengan alasan tercatat dan otorisasi berjenjang. |
| **BR-013** | **Validasi Anomali & Exception Blocking** | Must | Engine menjalankan pemeriksaan otomatis sebelum review (mis. net pay negatif, lonjakan gaji > ambang batas, profil pajak/rekening tidak lengkap) dengan tingkat keparahan (*Info/Warning/Blocker*). |

### 4.4 Domain Kepatuhan Indonesia (PPh 21, BPJS, THR, PKWT/PHK)

| ID | Kebutuhan Bisnis | Prioritas | Kriteria Keberhasilan / Acceptance Criteria |
|---|---|---|---|
| **BR-014** | **Perhitungan PPh Pasal 21 (TER & Masa Pajak Terakhir)** | Must | Sistem memetakan PTKP ke kategori TER (A/B/C), menghitung pajak bulanan Jan–Nov memakai tabel TER berversi, serta melakukan rekonsiliasi Pasal 17 pada masa pajak terakhir (Desember/berhenti) termasuk pengembalian lebih potong. |
| **BR-015** | **Dukungan Skema Pajak Gross, Gross-Up & Net** | Must | Sistem mampu menerapkan metode Gross, Gross-Up (tunjangan pajak konvergen otomatis), dan Net per kebijakan grup karyawan dengan perlakuan akuntansi dan bruto pajak yang sesuai regulasi. |
| **BR-016** | **Perhitungan & Rekonsiliasi BPJS Kesehatan** | Must | Sistem menghitung iuran PPU bagian pekerja dan pemberi kerja secara terpisah berdasarkan upah tetap dalam batas bawah (UMP/UMK) dan batas atas berversi, termasuk tambahan tanggungan keluarga, serta mendeteksi selisih terhadap tagihan resmi. |
| **BR-017** | **Perhitungan Terpisah 5 Program BPJS Ketenagakerjaan** | Must | Sistem memodelkan JHT, JKK (per tingkat risiko), JKM, JP (dengan batas atas upah berversi), dan JKP secara independen sesuai kelayakan usia/status peserta, serta memisahkan beban perusahaan vs potongan karyawan. |
| **BR-018** | **Pengelolaan THR Keagamaan (Permenaker 6/2016)** | Must | Sistem mengelola event THR, menghitung kelayakan dan masa kerja pada tanggal hari raya (penuh >= 12 bulan atau proporsional 1–<12 bulan), mengintegrasikan bruto THR ke perhitungan PPh 21 masa terkait, dan menerbitkan slip THR. |
| **BR-019** | **Offboarding, Final Settlement, Kompensasi PKWT & PHK** | Must | Sistem menghentikan komponen aktif saat resign/berakhir kontrak, menghitung prorata gaji terakhir, kompensasi sisa cuti (bila berlaku), uang kompensasi PKWT sesuai PP 35/2021, tabel hak PHK berbasis *decision table* tervalidasi Legal/HR, serta pelunasan sisa pinjaman. |

### 4.5 Domain Pinjaman, Pembayaran, Akuntansi, Pelaporan & Keamanan

| ID | Kebutuhan Bisnis | Prioritas | Kriteria Keberhasilan / Acceptance Criteria |
|---|---|---|---|
| **BR-020** | **Pengelolaan Pinjaman Karyawan & Potongan Sah** | Must | Sistem mengelola pengajuan, pencairan, jadwal cicilan otomatis per payroll run, pelacakan saldo terbayar/sisa, serta memblokir potongan bila melanggar batas minimum take-home pay atau tanpa otorisasi. |
| **BR-021** | **Payment Batch Bank, Idempotency & Rekonsiliasi** | Must | Sistem menghasilkan file transfer per bank untuk run `Approved`, memvalidasi rekening aktif, mencegah duplikasi pembayaran dengan *idempotency key*, dan mencatat status per penerima hingga `Reconciled` (termasuk antrean gagal transfer). |
| **BR-022** | **Integrasi Jurnal Akuntansi & Alokasi Cost Center** | Must | Sistem menghasilkan jurnal akuntansi seimbang (Debit = Kredit) yang memisahkan beban gaji, beban BPJS perusahaan, utang net pay, utang PPh 21, dan utang BPJS per cost center/departemen/cabang. |
| **BR-023** | **Penerbitan Slip Gaji & Bukti Potong** | Must | Sistem menghasilkan slip gaji PDF (terenkripsi/terproteksi akses) dan data ekspor bukti potong PPh 21 (BPA1/format DJP) yang konsisten 100% dengan payroll ledger. |
| **BR-024** | **Employee Self-Service (ESS) & Pelaporan Manajemen** | Should | Karyawan dapat mengakses portal untuk melihat slip gaji, saldo cuti, dan mengajukan permohonan dasar; Manajemen memperoleh laporan biaya karyawan, variance bulanan, dan tren lembur. |
| **BR-025** | **Tata Kelola Regulasi Berversi, Audit Trail & Keamanan** | Must | Seluruh tarif pajak/BPJS/UMP dikelola sebagai konfigurasi berversi dengan masa berlaku dan persetujuan (terpisah dari kebijakan internal perusahaan); seluruh aksi sensitif tercatat pada *immutable audit log*; data sensitif (NIK, NPWP, rekening, gaji) terenkripsi & termasking sesuai peran. |
| **BR-026** | **Migrasi Saldo Awal Tahun Berjalan (YTD Opening Balance)** | Must | Untuk *go-live* di tengah tahun takwim, sistem menyediakan modul impor saldo awal tervalidasi mencakup akumulasi bruto pajak, biaya jabatan, iuran JHT/JP pekerja, PPh 21 telah dipotong (Jan s/d bulan sebelum cutover), saldo awal cuti, dan sisa pinjaman karyawan. |

---

## 5. Asumsi Bisnis, Ketergantungan & Mitigasi Risiko

### 5.1 Asumsi Operasional
1. Perusahaan memiliki struktur organisasi, *Chart of Accounts* (COA), serta kebijakan internal tertulis (pola hari kerja, metode prorata, kategori komponen gaji, dan kebijakan pajak) yang difinalisasi sebelum fase implementasi teknis.
2. Seluruh transaksi payroll menggunakan mata uang Rupiah (IDR).
3. Data historis penghasilan dan pemotongan PPh 21 dalam tahun berjalan (untuk karyawan yang mulai di pertengahan tahun saat sistem go-live) tersedia untuk dimigrasi agar rekonsiliasi Desember tetap akurat.

### 5.2 Risiko Bisnis Utama & Strategi Mitigasi

| ID Risiko | Deskripsi Risiko | Dampak | Strategi Mitigasi |
|---|---|---|---|
| **RSK-01** | Perubahan regulasi mendadak (mis. perubahan batas atas JP tahunan, perubahan UMP/UMK, revisi aturan pajak DJP/Coretax). | Salah hitung massal dan denda keterlambatan/kurang bayar. | Terapkan *Regulatory Rule Versioning* (`BR-025`) dan SOP Pembaruan Regulasi (`SOP-PAY-006`) sehingga tarif dapat diperbarui tanpa deploy kode dan tetap menjaga histori lama. |
| **RSK-02** | Data master karyawan (PTKP, NIK/NPWP, nomor peserta BPJS, rekening) tidak valid saat go-live. | Gagal transfer gaji, penolakan pelaporan pajak/BPJS, atau salah tarif TER. | Terapkan *Pre-Run Input Validation* yang memisahkan *Warning* vs *Hard Blocker*, serta lakukan pembersihan data (*data cleansing*) sebelum migrasi. |
| **RSK-03** | Keterlambatan finalisasi absensi dan persetujuan lembur oleh atasan menjelang hari gajian. | Jadwal pembayaran gaji mundur (*SLA breach*). | Tetapkan kalender cutoff tegas (`SOP-PAY-001`), eskalasi otomatis pengingat approval H-2 cutoff, serta pemisahan penanganan kasus terlambat ke mekanisme rapel/periode berikutnya. |
| **RSK-04** | Kebocoran data gaji atau manipulasi rekening bank karyawan oleh oknum internal. | Kerugian finansial dan pelanggaran UU Perlindungan Data Pribadi (UU PDP). | Wajibkan *Maker-Checker-Approver* untuk perubahan rekening, enkripsi/masking data sensitif, pembatasan akses *SysAdmin*, dan pengujian keamanan akses horizontal (anti-IDOR). |
| **RSK-05** | Ketidakpercayaan pengguna terhadap hasil perhitungan sistem baru saat migrasi dari sistem lama. | Penolakan adopsi atau keterlambatan cutover. | Wajibkan fitur *Explainable Calculation Trace* (`BR-011`) dan lakukan minimal 2 siklus *Parallel Payroll Run* sebelum sistem lama dinonaktifkan. |
