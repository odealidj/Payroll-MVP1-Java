# Standard Operating Procedures (SOP) Operasional Penggajian
## Sistem Informasi Penggajian (Payroll Management System — Standar Indonesia)

**Dokumen ID:** `DOC-BA-SOP-001`  
**Versi:** 1.1  
**Tanggal:** 18 November 2026 (Revisi Pasca-Audit Internal BA/SA)  
**Acuan:** [02-BPA-Business-Process-Architecture-and-Flows.md](file:///home/aliube/Workspace/PLan/Payroll/docs/01-business-analysis/02-BPA-Business-Process-Architecture-and-Flows.md) & [03-Business-Rules-and-Calculation-Specification.md](file:///home/aliube/Workspace/PLan/Payroll/docs/01-business-analysis/03-Business-Rules-and-Calculation-Specification.md)

---

## Ringkasan Daftar SOP

| Kode SOP | Judul Prosedur Operasional | Pemilik Utama | SLA / Jadwal Baku |
|---|---|---|---|
| **SOP-PAY-001** | Kalender Cutoff & Persiapan Data Input Payroll Bulanan | HR Admin & Attendance Officer | H-7 s/d H-5 Kerja Sebelum Payday |
| **SOP-PAY-002** | Eksekusi Kalkulasi, Pemeriksaan Anomali & Otorisasi Payroll | Payroll Staff & Supervisor | H-4 s/d H-2 Kerja Sebelum Payday |
| **SOP-PAY-003** | Penanganan Koreksi Payroll, Off-Cycle Run & Rapel | Payroll Staff & Finance | Maks 3 Hari Kerja Setelah Temuan |
| **SOP-PAY-004** | Pencairan Gaji via Bank, Penanganan Retur & Rekonsiliasi | Finance Staff & Finance Manager | Payday (H-0) s/d H+2 Kerja |
| **SOP-PAY-005** | Rekonsiliasi, Pembayaran & Pelaporan PPh 21 & BPJS Bulanan | Tax Officer & BPJS Officer | BPJS Tgl 10/15; Pajak Setor Tgl 15 & Lapor Tgl 20 |
| **SOP-PAY-006** | Pembaruan Konfigurasi Regulasi & Kebijakan Secara Berversi | Tax / BPJS / Legal & HR Manager | Sebelum Tanggal Efektif Regulasi |
| **SOP-PAY-007** | Penutupan Akhir Tahun, Rollover Awal Tahun & Migrasi YTD | Tax, HR & Payroll Supervisor | Desember s/d Januari (atau Pra-Cutover) |

*(Catatan: Contoh tanggal `Payday` standar perusahaan misalkan tanggal **25** atau **akhir bulan**. Penjadwalan H-x mengikuti kalender hari kerja perusahaan).*

---

## SOP-PAY-001: Kalender Cutoff & Persiapan Data Input Payroll Bulanan

1. **Tujuan:** Memastikan seluruh perubahan kepegawaian, kompensasi, absensi, cuti, lembur, dan komponen variabel terkumpul lengkap dan disetujui sebelum tanggal *cutoff* sehingga kalkulasi payroll akurat tanpa revisi berulang.
2. **Ruang Lingkup:** Seluruh data input karyawan PKWTT dan PKWT untuk siklus payroll bulanan reguler.
3. **Pelaksana (PIC):** HR Administrator, Attendance Officer, Line Manager, Payroll Staff.
4. **Jadwal & SLA:**
   - **H-7 Hari Kerja:** Pengingat otomatis penutupan absensi, lembur, dan pengajuan perubahan data karyawan.
   - **H-5 Hari Kerja (Pukul 17.00 WIB):** Batas akhir (*Cutoff*) persetujuan data master HR, absensi, cuti, lembur, dan input variabel.
5. **Prosedur Langkah demi Langkah:**
   1. **H-10:** Payroll Staff membuka `PayrollPeriod` bulan berjalan dan memverifikasi tanggal *cutoff* serta daftar hari libur resmi pada `WorkCalendar`.
   2. **H-7:** HR Administrator memastikan seluruh karyawan baru (*joiner*), mutasi, promosi, perubahan gaji (*effective-dated*), dan pengakhiran kerja (*resign/habis kontrak/PHK*) telah memiliki dokumen persetujuan lengkap dari HR Manager.
   3. **H-6:** Attendance Officer mengimpor seluruh raw log absensi hingga tanggal *cutoff*, menyelesaikan anomali (*missing punch*), serta mengirimkan notifikasi kepada Line Manager yang belum menyetujui pengajuan cuti atau lembur timnya.
   4. **H-5 (Cutoff):** Attendance Officer menjalankan *Final Attendance Summary* dan mengunci periode absensi.
   5. **H-5:** Finance/HR mengunggah data variabel (bonus, insentif, reimbursement terpisah, potongan koperasi/pinjaman baru) yang telah disetujui ke dalam modul `PayrollInput`.
   6. **H-5:** Payroll Staff menjalankan *Pre-Run Input Validation* untuk mendeteksi data kurang (NIK/NPWP kosong, rekening belum terverifikasi, missing PTKP) dan menugaskan pemilik data menyelesaikannya dalam 1x24 jam.
6. **Penanganan Pengecualian:** Pengajuan lembur atau perubahan gaji yang disetujui melewati pukul 17.00 pada hari *cutoff* (H-5) **dilarang** dimasukkan ke periode berjalan secara informal; transaksi tersebut otomatis dialihkan ke antrean **rapel (*retroactive adjustment*) periode berikutnya** kecuali ada izin tertulis dari HR Manager & Finance Manager.

---

## SOP-PAY-002: Eksekusi Kalkulasi, Pemeriksaan Anomali & Otorisasi Payroll

1. **Tujuan:** Menjalankan mesin kalkulasi payroll secara terkontrol dan memastikan pemisahan tugas *Maker–Checker–Approver* sebelum dana gaji dicairkan.
2. **Ruang Lingkup:** Payroll Run Reguler Bulanan, THR Run, dan Final Payroll Run.
3. **Pelaksana (PIC):** Payroll Staff (*Maker*), Payroll Supervisor, Tax Officer, BPJS Officer (*Checker*), Finance Manager & Director (*Approver*).
4. **Jadwal & SLA:** H-4 s/d H-2 Hari Kerja sebelum *Payday*.
5. **Prosedur Langkah demi Langkah:**
   1. **Maker (H-4 Pagi):** Payroll Staff membuat `PayrollRun` (status `Draft`), membekukan **Snapshot Input** dan memastikan **`rule_set_version`** aktif untuk pajak & BPJS sesuai tanggal periode.
   2. **Eksekusi Engine:** Payroll Staff menjalankan kalkulasi. Status berubah menjadi `Calculated`.
   3. **Pemeriksaan Anomali (H-4 Siang):** Payroll Staff meninjau laporan *Pre-Approval Exception & Variance*:
      - Pastikan **0 Blocker** (`Net Pay < 0`, missing tax/BPJS rule).
      - Selidiki setiap karyawan dengan *Warning* (kenaikan/penurunan Net Pay > ±20% atau perubahan rekening bank < H-5).
   4. **Checker Stage 1 — Payroll Supervisor (H-3 Pagi):** Memeriksa sampel *Calculation Trace* (khususnya joiner, leaver, kenaikan gaji tengah bulan, lembur libur nasional, dan masa pajak Desember), mencocokkan total *headcount* dan *Gross-to-Net* terhadap bulan lalu (*Payroll Variance Report*).
   5. **Checker Stage 2 — Tax & BPJS Officer (H-3 Siang):** Tax Officer memverifikasi kewajaran PPh 21 (kategori TER / Pasal 17) dan metode Gross/Gross-Up/Net; BPJS Officer memverifikasi kewajaran dasar upah dan iuran per program.
   6. **Revisi Input (Bila Ada Temuan):** Jika ditemukan kesalahan input sebelum persetujuan, status dikembalikan ke `Draft/Input Validation`, data sumber diperbaiki beserta dokumen pendukung, lalu dibuat snapshot baru dan dihitung ulang.
   7. **Checker Stage 3 — Finance Manager (H-2 Pagi):** Memeriksa ringkasan *Total Employer Cost*, kecukupan kas bank, dan alokasi *cost center*.
   8. **Approver — Director / Authorized Approver (H-2 Sore):** Memberikan persetujuan digital akhir. Sistem mengubah status run menjadi **`Approved` (Immutable/Locked)** dan mencatat timestamp serta *approval reference* di `AuditLog`.
6. **Dokumen Terkait:** *Payroll Register*, *Payroll Variance Report*, *Tax & BPJS Summary*, *Exception Sign-Off Log*.

---

## SOP-PAY-003: Penanganan Koreksi Payroll, Off-Cycle Run & Perhitungan Rapel

1. **Tujuan:** Memberikan prosedur yang aman dan teraudit untuk memperbaiki kekurangan/kelebihan bayar atau memproses gaji susulan setelah `PayrollRun` berstatus `Approved`/`Paid`.
2. **Prinsip Wajib:** **Dilarang keras mengedit, menghapus, atau menimpa data pada `PayrollRun` yang sudah `Approved` atau `Paid`.**
3. **Pelaksana (PIC):** HR Admin, Payroll Staff, Payroll Supervisor, Tax Officer, Finance Manager, Director.
4. **Prosedur Langkah demi Langkah:**
   1. **Identifikasi Kasus:** Klasifikasikan jenis koreksi:
      - *Tipe A — Keterlambatan Input / Kenaikan Gaji Berlaku Surut:* Gunakan **Retroactive Adjustment (Rapel)** pada periode reguler berikutnya.
      - *Tipe B — Karyawan Terlewat Gaji / Kesalahan Material Mendesak Sebelum Payday Berikut:* Gunakan **Off-Cycle Payroll Run**.
      - *Tipe C — Kesalahan Fatal Batch Sebelum Dana Dicairkan:* Gunakan **Reversal Run** dan buat run pengganti.
   2. **Permohonan Koreksi:** Payroll Staff/HR membuat tiket *Payroll Adjustment Request* yang mencantumkan `reference_run_id` asal, daftar karyawan terdampak, selisih nominal (sebelum vs sesudah), alasan, dan bukti persetujuan HR.
   3. **Kalkulasi Dampak Turunan:** Engine menghitung ulang dampak terhadap PPh 21 (penyesuaian bruto pajak pada masa pajak saat koreksi dibayarkan sesuai PMK 168/2023) dan BPJS (jika berdampak pada upah tetap dan tagihan bulan berjalan/pembetulan).
   4. **Otorisasi:** Diperiksa oleh Payroll Supervisor & Tax Officer, serta disetujui oleh Finance Manager & Director.
   5. **Eksekusi & Pelacakan:** Setelah disetujui, nilai masuk ke batch pembayaran atau digabungkan pada slip gaji periode berjalan dengan label komponen eksplisit (mis. *"Rapel Gaji Pokok Periode Okt"*).

---

## SOP-PAY-004: Pencairan Gaji via Bank, Penanganan Retur & Rekonsiliasi Bank

1. **Tujuan:** Memastikan pencairan gaji tepat nominal, tepat rekening penerima, bebas duplikasi, dan terekonsiliasi dengan mutasi rekening koran perusahaan.
2. **Pelaksana (PIC):** Finance Staff (*Maker*), Finance Manager (*Checker*), Authorized Bank Signatory (*Approver*).
3. **Jadwal & SLA:**
   - **H-1 Hari Kerja:** Pembuatan & pemeriksaan *Payment Batch File*.
   - **H-0 (*Payday*):** Rilis transfer bank sebelum batas kliring/BI-FAST/payroll bank.
   - **H+1 s/d H+2 Hari Kerja:** Rekonsiliasi status transfer & penerbitan bukti bayar final.
4. **Prosedur Langkah demi Langkah:**
   1. **Validasi Pra-Batch (H-1):** Sistem menarik `PaymentInstruction` dari `PayrollRun` berstatus `Approved`. Karyawan dengan status rekening `Invalid` atau `Pending Verification` otomatis berstatus **`On Hold`** dan tidak masuk file bank.
   2. **Generate Batch File (Maker — Finance Staff):** Finance Staff mengelompokkan instruksi per bank pembayar dan tanggal bayar. Sistem membubuhkan **`idempotency_key`** unik per instruksi agar tidak bisa diproses dua kali. File hasil ekspor dienkripsi/dibatasi akses unduhnya.
   3. **Verifikasi Total (Checker — Finance Manager):** Mencocokkan `Total Nominal Batch File` + `Total Hold` == `Total Approved Net Pay` pada *Payroll Register*.
   4. **Pelepasan Dana (Approver — Bank Signatory):** Pejabat berwenang mengunggah/menyetujui batch melalui portal *Corporate Internet Banking*. Status instruksi berubah menjadi `Submitted`.
   5. **Penerbitan Slip Gaji:** Setelah status bank mengonfirmasi transaksi berhasil (`Paid`), sistem menerbitkan *Payslip PDF* terproteksi ke portal ESS / email karyawan pada hari *Payday*.
   6. **Rekonsiliasi & Penanganan Gagal Transfer (H+1):**
      - Finance Staff mengimpor *Bank Response File* / mutasi rekening koran ke sistem.
      - Transaksi sukses ditandai **`Reconciled`**.
      - Transaksi gagal/retur (mis. rekening tutup/dormant atau nama tidak cocok) ditandai **`Failed`** dan masuk **Payment Exception Queue**.
      - HR/Payroll menghubungi karyawan untuk pembaruan & verifikasi rekening (mengikuti aturan approval perubahan rekening pada `BPA-02`).
      - Setelah rekening terverifikasi, Finance Staff membuat **Re-payment Batch** khusus transaksi `Failed` tersebut (dengan *idempotency key* baru yang merujuk instruksi asal) hingga seluruh karyawan berstatus `Reconciled`.
   7. **Posting Jurnal Akuntansi:** Finance Staff meninjau draf jurnal pengakuan beban/utang & jurnal pembayaran bank per *cost center*, lalu mem-posting ke modul/file ekspor akuntansi.

---

## SOP-PAY-005: Rekonsiliasi, Pembayaran & Pelaporan Kewajiban PPh 21 & BPJS Bulanan

1. **Tujuan:** Memastikan seluruh potongan karyawan dan beban perusahaan atas PPh 21 serta BPJS disetor dan dilaporkan tepat waktu tanpa denda administrasi.
2. **Pelaksana (PIC):** BPJS Officer, Tax Officer, Finance Staff, Finance Manager.
3. **Jadwal & SLA Regulasi Indonesia:**
   - **BPJS Kesehatan:** Rekonsiliasi mutasi peserta sebelum tanggal *cut-off* bulanan e-Dabu; pembayaran paling lambat **tanggal 10** bulan berjalan/berikutnya sesuai ketentuan tagihan Virtual Account.
   - **BPJS Ketenagakerjaan:** Rekonsiliasi data SIPP; pembayaran iuran paling lambat **tanggal 15** bulan berikutnya.
   - **PPh Pasal 21 (PMK 81/2024 — Coretax):** Penyetoran pajak (pembuatan ID Billing & bayar) paling lambat **tanggal 15 bulan berikutnya** (jika jatuh pada hari libur, bergeser ke hari kerja berikutnya); Pelaporan SPT Masa & penerbitan Bukti Potong paling lambat **tanggal 20 bulan berikutnya**.
4. **Prosedur Langkah demi Langkah:**
   1. **Rekonsiliasi BPJS (H+1 s/d Tanggal 5):**
      - BPJS Officer mengunduh rekap perhitungan internal dari sistem (dipisahkan antara BPJS Kesehatan dan program BPJS TK yang berbayar: JHT, JKK, JKM, JP).
      - Cocokkan dengan tagihan resmi dari portal **e-Dabu** dan **SIPP**.
      - Jika ada selisih (mis. karyawan resign terlambat dilaporkan ke BPJS atau ada perbedaan dasar upah), catat dalam *BPJS Reconciliation Exception* dan tindak lanjuti melalui perbaikan data kepesertaan/penyesuaian payroll periode berikut (**dilarang memaksa ubah angka payroll lama**).
   2. **Rekonsiliasi PPh 21 (Tanggal 1 s/d Tanggal 12):**
      - Tax Officer mengekspor *PPh 21 Monthly Summary* dan memverifikasi bahwa total utang PPh 21 di jurnal akuntansi sama persis dengan total pajak terutang pada daftar bukti potong.
      - Khusus bulan **Desember** (atau karyawan keluar), Tax Officer memverifikasi hasil rekonsiliasi Pasal 17 dan memastikan seluruh kasus **Lebih Potong** telah dikembalikan (*refund*) ke karyawan dan/atau ditangani sesuai aturan DJP.
   3. **Pembayaran Kewajiban & Pencatatan NTPN (Maksimal Tanggal 10 BPJS Kes / Tanggal 15 BPJS TK & PPh 21):** Finance Staff membuat instruksi bayar ke Kas Negara (Billing Pajak) dan VA BPJS, disetujui Finance Manager, serta mencatat **NTPN / bukti bayar resmi** ke entitas `statutory_payments`.
   4. **Pelaporan & Arsip (Maksimal Tanggal 20):** Tax Officer mengimpor data bukti potong dari sistem ke portal resmi DJP/Coretax, melakukan submit SPT Masa, serta mengarsipkan BPE (Bukti Penerimaan Elektronik) di dalam sistem dengan masa retensi minimal **10 tahun**.

---

## SOP-PAY-006: Pembaruan Konfigurasi Regulasi Secara Berversi

1. **Tujuan:** Mengelola perubahan peraturan pemerintah (tarif TER, PTKP, batas atas upah JP/BPJS Kesehatan, UMP/UMK tahunan, aturan lembur/pesangon) secara terkendali tanpa mengubah kode aplikasi atau merusak histori payroll masa lalu.
2. **Pelaksana (PIC):** Tax Officer / BPJS Officer / Legal (*Maker*), Payroll Supervisor (*Checker*), HR Manager & Finance Manager (*Approver*).
3. **Jadwal & SLA:** Maksimal 5 hari kerja sebelum tanggal mulai berlaku peraturan baru (atau segera setelah regulasi resmi diundangkan bila berlaku surut).
4. **Prosedur Langkah demi Langkah:**
   1. **Pemantauan & Kajian Regulasi:** Tax/BPJS/Legal memantau naskah resmi dari JDIH Kemnaker, DJP, Kemenkeu, dan BPJS. Catat nomor peraturan, pasal/lampiran, tautan resmi, dan tanggal berlaku efektif (`effective_from`).
   2. **Analisis Dampak:** Tim mengidentifikasi tabel parameter mana yang berubah (mis. `jp_max_wage_cap`, `regional_minimum_wage`, `tax_ter_brackets`) dan kelompok karyawan terdampak.
   3. **Input Versi Konfigurasi Baru (Maker):** Officer membuat versi konfigurasi baru (`rule_set_version` baru berstatus `Draft`) dengan mengisi `effective_from` baru, tanpa menghapus atau mengubah baris versi lama (sistem otomatis menutup `effective_to` versi lama pada H-1).
   4. **Pengujian Simulasi (Checker):** Payroll Supervisor & QA/Tax menjalankan *regression test case* (kasus uji standar dan kasus batas) pada lingkungan simulasi/staging untuk memastikan perhitungan sesuai contoh resmi pemerintah.
   5. **Persetujuan & Aktivasi (Approver):** HR Manager & Finance Manager menyetujui `rule_set_version` baru hingga berstatus `Active`. Perubahan tercatat pada `AuditLog` lengkap dengan dokumen peraturan yang dilampirkan.
   6. **Jaminan Histori:** Payroll run periode sebelum `effective_from` tetap mengacu pada `rule_set_version` lama secara otomatis.

---

## SOP-PAY-007: Penutupan Akhir Tahun, Rollover Awal Tahun & Migrasi Saldo Awal YTD

1. **Tujuan:** Memastikan kelengkapan rekonsiliasi pajak tahunan (BPA1), rollover saldo cuti & aturan tahun baru, serta akurasi migrasi saldo awal (*Year-to-Date / YTD*) bila sistem *go-live* di pertengahan tahun.
2. **Pelaksana (PIC):** Tax Officer, HR Admin, HR Manager, Payroll Supervisor.
3. **Prosedur Bagian A — Migrasi Saldo Awal YTD (Saat Go-Live Tengah Tahun):**
   1. Sebelum *payroll run* pertama berjalan di sistem baru, Payroll Staff dan Tax Officer menyiapkan templat impor `employee_ytd_opening_balances` berisi akumulasi bulan Januari s/d bulan sebelum cutover: `ytd_gross_taxable`, `ytd_biaya_jabatan`, `ytd_bpjs_worker_pension`, `ytd_pph21_withheld`, ditambah saldo cuti pembukaan (`OPENING` di `leave_transactions`) dan sisa pokok pinjaman.
   2. Tax Officer mencocokkan total PPh 21 pada data migrasi terhadap total SPT Masa PPh 21 yang telah disetor dan dilaporkan ke DJP pada bulan-bulan sebelumnya.
   3. HR Manager dan Finance Manager menyetujui unggahan YTD sehingga terkunci sebagai saldo awal resmi.
4. **Prosedur Bagian B — Penutupan Akhir Tahun (Desember) & Rollover (Januari):**
   1. **Desember (Minggu ke-3):** Jalankan `PayrollRun` Desember sebagai Masa Pajak Terakhir; sistem otomatis menjumlahkan seluruh run Januari–Desember (+ YTD migrasi bila ada), menghitung Pasal 17 setahun, dan membayarkan *refund* Lebih Potong bila negatif.
   2. **Januari (Tgl 1–5):** Tax Officer menerbitkan daftar **Bukti Potong BPA1** untuk seluruh karyawan tetap (aktif maupun yang berhenti sepanjang tahun lalu) melalui format ekspor Coretax.
   3. **Januari (Tgl 1–5):** HR Admin menjalankan proses *Year-End Leave Rollover* (mencatat `CARRY_FORWARD` dan `EXPIRY` sesuai kebijakan cuti), mengaktifkan `WorkCalendar`, `UMP/UMK/UMSP`, serta batas upah BPJS TK tahun baru (`SOP-PAY-006`), dan memutakhirkan perubahan PTKP per 1 Januari pada `TaxProfile`.
