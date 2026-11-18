# Katalog Business Rules & Spesifikasi Perhitungan (Calculation Specification)
## Sistem Informasi Penggajian (Payroll Management System — Standar Indonesia)

**Dokumen ID:** `DOC-BA-BR-001`  
**Versi:** 1.1  
**Tanggal:** 18 November 2026 (Revisi Pasca-Audit Internal BA/SA)  
**Acuan Regulasi:** UU Ketenagakerjaan jo. UU Cipta Kerja, PP 35/2021, PP 36/2021 jo. PP 51/2023 jo. PP 49/2025, Permenaker 6/2016, UU HPP, PP 58/2023, PMK 168/2023, PMK 81/2024, PP 68/2009, Perpres 64/2020, Regulasi BPJS Ketenagakerjaan.

> **Catatan Tata Kelola Regulasi vs Kebijakan Perusahaan:** Seluruh tarif hukum, lapisan PKP, kategori TER, dan batas upah **wajib disimpan sebagai tabel parameter berversi (`LegalRuleVersion`)**, sedangkan kebijakan internal perusahaan (metode prorata default, pembulatan komponen, batas lembur internal, ambang *variance*) disimpan terpisah pada **`CompanyPolicyVersion`**, bukan *hard-coded*. Sebelum go-live, Tax Officer, BPJS Officer, dan Legal wajib mencocokkan parameter awal terhadap naskah primer resmi terbaru.

---

## 0. Aturan Hubungan Kerja, Masa Percobaan, Cuti Sakit & Upah Minimum (`BR-EMP` & `BR-LEAVE`)

| Rule ID | Nama Aturan | Ketentuan & Validasi Sistem |
|---|---|---|
| **BR-EMP-001** | **Larangan Masa Percobaan pada PKWT** | Sesuai Pasal 58 UU Ketenagakerjaan, **kontrak PKWT dilarang mencantumkan masa percobaan (*probation*)**; jika terisi pada input kontrak PKWT, sistem wajib menolak (`HARD BLOCK`). Masa percobaan hanya sah untuk **PKWTT** maksimal **3 bulan** dengan upah tetap minimal sebesar UMP/UMK/UMSP yang berlaku. |
| **BR-EMP-002** | **Validasi Upah Minimum Regional & Sektoral (PP 49/2025)** | Mengacu PP 36/2021 jo. PP 49/2025, sistem memvalidasi upah tetap terhadap Upah Minimum Provinsi/Kabupaten-Kota (**UMP/UMK**) serta **Upah Minimum Sektoral (UMSP/UMSK)** bila perusahaan berada pada sektor yang ditetapkan pemerintah daerah. |
| **BR-LEAVE-001** | **Klasifikasi Cuti & Perlakuan Upah** | Jenis cuti dikelompokkan dengan flag eksplisit `is_paid`: Cuti Tahunan (`paid`), Cuti Sakit dengan surat dokter (`paid` bertahap), Cuti Melahirkan/Keguguran (`paid` sesuai UU ketenagakerjaan/UU KIA), Izin Penting/Menikah/Duka/Haid (`paid` sesuai ketentuan PP/PKB), dan *Unpaid Leave* (`unpaid`, mengurangi komponen prorata). |
| **BR-LEAVE-002** | **Skala Upah Sakit Berkepanjangan** | Mengacu Pasal 93 UU Ketenagakerjaan: bagi pekerja yang sakit berkepanjangan dengan surat keterangan dokter sah, pembayaran upah mengikuti skala bertahap: **4 bulan pertama = 100%**, **4 bulan kedua = 75%**, **4 bulan ketiga = 50%**, dan **4 bulan selanjutnya = 25%** dari upah sebelum pemutusan hubungan kerja dilakukan oleh pengusaha. |

---

## 1. Aturan Effective Dating, Prorata & Multi-Segment (`BR-PRO`)

| Rule ID | Nama Aturan | Definisi & Formula Matematis |
|---|---|---|
| **BR-PRO-001** | **Resolusi Data Berbasis Effective Date** | Untuk setiap periode payroll `[period_start, period_end]`, sistem mengambil data organisasi, kontrak, status PTKP, dan struktur gaji yang memiliki irisan tanggal berlaku (`valid_from <= period_end` DAN `(valid_to IS NULL OR valid_to >= period_start)`). |
| **BR-PRO-002** | **Metode Prorata Hari Kerja Terjadwal (Default)** | Digunakan untuk karyawan masuk/berhenti di tengah bulan atau *unpaid leave*.<br>**Rumus:** `Nilai Prorata = (Hari Kerja Aktual Dibayar / Total Hari Kerja Terjadwal dalam Periode) × Nilai Komponen Sebulan` |
| **BR-PRO-003** | **Metode Prorata Hari Kalender (Konfigurasi Alternatif)** | Digunakan jika kebijakan komponen ditetapkan memakai hari kalender.<br>**Rumus:** `Nilai Prorata = (Jumlah Hari Kalender Masa Kerja Aktif dalam Periode / Jumlah Hari Kalender Sebulan) × Nilai Komponen Sebulan` |
| **BR-PRO-004** | **Multi-Segment Split Tengah Periode** | Jika terjadi perubahan nominal komponen gaji di tengah periode (mis. `valid_from` = tgl 16 pada periode 1–30), periode dipecah menjadi $n$ segmen berurutan `[tgl 1–15]` dan `[tgl 16–30]`. Setiap segmen dihitung sesuai `BR-PRO-002` atau `BR-PRO-003`, lalu dijumlahkan: `Total Komponen = Σ Segmen_i`. |

### Contoh Numerik `BR-PRO-004`:
- Periode November: 21 hari kerja terjadwal (tgl 1–15 = 11 hari kerja, tgl 16–30 = 10 hari kerja).
- Gaji Pokok tgl 1–15 = Rp8.000.000; naik per tgl 16 menjadi Rp10.000.000.
- Segmen 1 = `(11 / 21) × 8.000.000 = Rp4.190.476,19`
- Segmen 2 = `(10 / 21) × 10.000.000 = Rp4.761.904,76`
- Total Gaji Pokok = `Rp8.952.380,95` -> dibulatkan sesuai aturan komponen (mis. ke rupiah terdekat = `Rp8.952.381`).

---

## 2. Aturan Komponen Gaji Multi-Dimensi (`BR-COMP`)

| Rule ID | Nama Aturan | Spesifikasi Aturan |
|---|---|---|
| **BR-COMP-001** | **Pemisahan Atribut Perlakuan** | Setiap `SalaryComponent` dilarang hanya memiliki 1 flag `taxable/bpjs`. Wajib memiliki matriks atribut independen:<br>1. `category`: `EARNING`, `DEDUCTION`, `EMPLOYER_CONTRIBUTION`, `REIMBURSEMENT`, `MEMO`<br>2. `is_fixed`: `true/false` (menentukan upah tetap vs tidak tetap)<br>3. `tax_treatment`: `TAXABLE_REGULAR`, `TAXABLE_IRREGULAR`, `NON_TAXABLE`, `TAX_ALLOWANCE`<br>4. `is_bpjs_kes_base`, `is_bpjs_jht_base`, `is_bpjs_jkk_jkm_base`, `is_bpjs_jp_base`<br>5. `is_overtime_base`, `is_thr_base`, `is_pkwt_comp_base`<br>6. `prorate_method`: `NONE`, `WORKING_DAYS`, `CALENDAR_DAYS`<br>7. `rounding_mode`: `HALF_UP_UNIT`, `FLOOR_UNIT`, `CEIL_THOUSAND`, dll. |
| **BR-COMP-002** | **Validasi Proporsi Upah Tetap (PP 36/2021)** | Apabila struktur upah terdiri dari Gaji Pokok dan Tunjangan Tetap, sistem memvalidasi peringatan/aturan bahwa Gaji Pokok minimal 75% dari jumlah Gaji Pokok + Tunjangan Tetap (kecuali diatur lain sesuai regulasi/pengecualian khusus perusahaan). |
| **BR-COMP-003** | **Pemisahan Reimbursement** | Komponen berkategori `REIMBURSEMENT` (penggantian biaya riil dengan bukti seperti klaim medis/perjalanan dinas) tidak dimasukkan ke `Gross Earnings` upah, tidak menjadi basis BPJS/lembur/THR, dan dikecualikan dari bruto PPh 21 sesuai ketentuan fiskal. |

---

## 3. Aturan Perhitungan Upah Lembur (`BR-OT` — PP 35 Tahun 2021)

| Rule ID | Nama Aturan | Dasar & Formula |
|---|---|---|
| **BR-OT-001** | **Kelayakan & Pra-Persetujuan** | Pekerja pada golongan jabatan tertentu (yang menurut PP 35/2021 dikecualikan karena tugas/tanggung jawab dengan upah lebih tinggi, misalnya level Manajer ke atas sesuai PP/PKB perusahaan) ditandai `overtime_eligible = false`. Untuk pekerja berhak, hanya lembur berstatus `Approved` (pra-persetujuan + verifikasi aktual) yang dihitung. |
| **BR-OT-002** | **Dasar Upah Sejam (`Hourly Rate`)** | `Upah Sejam = (1 / 173) × Dasar Upah Lembur Sebulan` *(PP 35/2021 Pasal 32)*<br>Dimana `Dasar Upah Lembur Sebulan`:<br>- Jika upah terdiri atas Gaji Pokok + Tunjangan Tetap: **`100% × (Gaji Pokok + Tunjangan Tetap)`**.<br>- Jika upah terdiri atas Gaji Pokok + Tunjangan Tetap + Tunjangan Tidak Tetap, dan porsi `(Gaji Pokok + Tunjangan Tetap) < 75%` dari total upah keseluruhan: **`75% × (Gaji Pokok + Tunjangan Tetap + Tunjangan Tidak Tetap)`**.<br>*(Apabila porsi Gaji Pokok + Tunjangan Tetap sudah >= 75% dari total upah, dasar tetap menggunakan 100% dari komponen tetap sesuai teks regulasi).* |
| **BR-OT-003** | **Pengali Lembur Hari Kerja Biasa** | - Jam lembur ke-1: **1,5 × Upah Sejam**<br>- Jam lembur ke-2 dan seterusnya: **2,0 × Upah Sejam** |
| **BR-OT-004** | **Pengali Lembur Hari Istirahat Mingguan / Libur Resmi (Pola 5 Hari Kerja)** | - Jam ke-1 s/d ke-8: **2,0 × Upah Sejam**<br>- Jam ke-9: **3,0 × Upah Sejam**<br>- Jam ke-10 s/d ke-12 (sesuai batas PP 35/2021): **4,0 × Upah Sejam** |
| **BR-OT-005** | **Pengali Lembur Hari Istirahat Mingguan / Libur Resmi (Pola 6 Hari Kerja)** | **Hari Istirahat Mingguan / Libur Resmi Umum (7 jam kerja normal):**<br>- Jam ke-1 s/d ke-7: **2,0 × Upah Sejam**<br>- Jam ke-8: **3,0 × Upah Sejam**<br>- Jam ke-9 s/d ke-11: **4,0 × Upah Sejam**<br>**Khusus Hari Libur Resmi yang jatuh pada Hari Kerja Terpendek (mis. Jumat, 5 jam normal):**<br>- Jam ke-1 s/d ke-5: **2,0 × Upah Sejam**<br>- Jam ke-6: **3,0 × Upah Sejam**<br>- Jam ke-7 s/d ke-9: **4,0 × Upah Sejam**<br>*(Validasi batas maksimal jam lembur sesuai PP 35/2021: maks 4 jam/hari dan 18 jam/minggu pada hari kerja normal).* |

### Contoh Numerik `BR-OT-002` & `BR-OT-004`:
- Gaji Pokok + Tunjangan Tetap = Rp8.650.000 -> `Upah Sejam = 8.650.000 / 173 = Rp50.000`.
- Lembur di Hari Libur Nasional pada pola 5 hari kerja selama 10 jam:
  - 8 jam pertama: `8 × 2,0 × 50.000 = Rp800.000`
  - Jam ke-9: `1 × 3,0 × 50.000 = Rp150.000`
  - Jam ke-10: `1 × 4,0 × 50.000 = Rp200.000`
  - Total Upah Lembur = `Rp1.150.000`.

---

## 4. Aturan Perhitungan BPJS Kesehatan (`BR-BPJS-KES`)

| Rule ID | Nama Aturan | Formula & Parameter Berversi |
|---|---|---|
| **BR-BPJS-KES-001** | **Dasar Upah & Batas Upah** | `Basis Awal = Σ Komponen (is_bpjs_kes_base = true)` (umumnya Gaji Pokok + Tunjangan Tetap).<br>`Basis Terbatas = MIN(MAX(Basis Awal, UMP_UMK_Lokasi), bpjs_kes_max_wage_cap)`<br>*(Parameter default berversi saat ini: `bpjs_kes_max_wage_cap = Rp12.000.000`, `UMP/UMK` mengikuti lokasi kerja karyawan)*. |
| **BR-BPJS-KES-002** | **Tarif PPU (Pekerja Penerima Upah)** | - **Bagian Pemberi Kerja (Employer Expense):** `4,0% × Basis Terbatas` (tidak memotong gaji pekerja, masuk penambah bruto PPh 21).<br>- **Bagian Pekerja (Employee Deduction):** `1,0% × Basis Terbatas` (memotong take-home pay). |
| **BR-BPJS-KES-003** | **Tambahan Anggota Keluarga (Ke-4 dst)** | Tarif 5% (4% + 1%) mencakup pekerja + pasangan + maksimal 3 anak. Jika karyawan mendaftarkan keluarga tambahan (anak ke-4, orang tua, atau mertua), dikenakan tambahan **`1,0% × Basis Terbatas` per orang**, yang seluruhnya menjadi potongan gaji pekerja (*Employee Deduction*). |
| **BR-BPJS-KES-004** | **Aturan Tanggal Potong Kepesertaan Baru** | Karyawan baru yang didaftarkan/aktif setelah tanggal *cut-off* kepesertaan BPJS Kesehatan bulan berjalan (sesuai tagihan e-Dabu) ditangani berdasarkan status aktif kepesertaan pada bulan tagihan untuk menghindari pemotongan tanpa tagihan resmi. |

---

## 5. Aturan Perhitungan BPJS Ketenagakerjaan — 5 Program Terpisah (`BR-BPJS-TK`)

Setiap program wajib dihitung dalam baris perhitungan terpisah (`payroll_detail` tersendiri):

| Rule ID | Program | Pihak Penanggung & Tarif Berversi | Batas Upah & Kelayakan |
|---|---|---|---|
| **BR-BPJS-TK-001** | **JHT (Jaminan Hari Tua)** | - Pemberi Kerja: **3,7%**<br>- Pekerja (Potongan): **2,0%** | Tanpa batas atas upah. Berlaku bagi WNI maupun WNA (> 6 bulan kerja). |
| **BR-BPJS-TK-002** | **JKK (Jaminan Kecelakaan Kerja)** | - Pemberi Kerja: **0,24% / 0,54% / 0,89% / 1,27% / 1,74%** (sesuai tingkat risiko industri perusahaan)<br>- Pekerja: **0%** | Tanpa batas atas upah. Menambah Penghasilan Bruto PPh 21 karyawan. |
| **BR-BPJS-TK-003** | **JKM (Jaminan Kematian)** | - Pemberi Kerja: **0,30%**<br>- Pekerja: **0%** | Tanpa batas atas upah. Menambah Penghasilan Bruto PPh 21 karyawan. |
| **BR-BPJS-TK-004** | **JP (Jaminan Pensiun)** | - Pemberi Kerja: **2,0%**<br>- Pekerja (Potongan): **1,0%** | **Ada batas atas upah (`jp_max_wage_cap`)** yang diperbarui tiap tahun oleh BPJS TK (parameter referensi awal: **Rp11.086.300** efektif 1 Maret 2026 — *wajib dicocokkan ke pengumuman resmi BPJS TK*). Dikecualikan untuk WNA & pekerja yang telah melewati usia pensiun sesuai ketentuan BPJS TK. |
| **BR-BPJS-TK-005** | **JKP (Jaminan Kehilangan Pekerjaan — Status Kepesertaan)** | **Tidak ada tambahan iuran bagi Pekerja maupun Pemberi Kerja.** Sumber pendanaan berasal dari kontribusi Pemerintah (0,22%) dan rekomposisi internal BPJS atas iuran JKK (0,14%) & JKM (0,10%) yang sudah termasuk dalam tarif JKK/JKM standar. | **Model di Engine:** JKP dimodelkan sebagai **status kepesertaan & hak manfaat (`is_jkp_eligible`)** (untuk WNI usia < 54 tahun saat mendaftar dengan kepesertaan penuh), **BUKAN** sebagai baris potongan maupun baris beban tambahan perusahaan di atas tarif JKK (`0,24%–1,74%`) dan JKM (`0,30%`) guna mencegah *double-counting*. |
| **BR-BPJS-TK-006** | **Pemisahan Akuntansi & Net Pay** | Hanya bagian pekerja (JHT 2% + JP 1%) yang mengurangi *Net Pay*. Bagian pemberi kerja dicatat ke akun Beban & Utang BPJS Perusahaan. |
| **BR-BPJS-TK-007** | **Satu Kali Kalkulasi Iuran per Bulan (`Single Monthly Deduction`)** | Apabila dalam 1 bulan takwim terdapat lebih dari satu `PayrollRun` (mis. `THR Run` lalu `Regular Run`), iuran BPJS Kesehatan dan seluruh program BPJS Ketenagakerjaan **hanya dihitung dan dipotong 1 kali pada bulan tersebut** (umumnya pada `REGULAR` run). | |

---

## 6. Aturan Perhitungan PPh Pasal 21 (`BR-TAX` — PP 58/2023 & PMK 168/2023)

### 6.1 Pemetaan Status PTKP & Kategori TER (`BR-TAX-001`)

| Status PTKP | Nilai PTKP Setahun (Rp) | Kategori TER |
|---|---|---|
| `TK/0` | Rp 54.000.000 | **TER A** |
| `TK/1` | Rp 58.500.000 | **TER A** |
| `K/0` | Rp 58.500.000 | **TER A** |
| `TK/2` | Rp 63.000.000 | **TER B** |
| `TK/3` | Rp 67.500.000 | **TER B** |
| `K/1` | Rp 63.000.000 | **TER B** |
| `K/2` | Rp 67.500.000 | **TER B** |
| `K/3` | Rp 72.000.000 | **TER C** |

> Status PTKP diambil per kondisi pada **awal tahun takwim (1 Januari)** atau **awal bulan mulai bekerja** jika bergabung di tengah tahun. Perubahan status keluarga di tengah tahun baru berlaku pada tahun pajak berikutnya (1 Januari tahun depan).

### 6.2 Komponen Penghasilan Bruto Pajak (`BR-TAX-002`)
`Bruto Pajak Sebulan = Σ Earning Objek Pajak (Gaji Pokok + Tunjangan + Lembur + Bonus + THR + Tunjangan Pajak jika Gross-Up) + Premi JKK Pemberi Kerja + Premi JKM Pemberi Kerja + Iuran BPJS Kesehatan Pemberi Kerja (4%)`

*(Catatan: Iuran JHT 3,7% dan JP 2% yang dibayar pemberi kerja **tidak** termasuk penambah penghasilan bruto pegawai; BPJS TK JHT 2% dan JP 1% yang dibayar pekerja baru menjadi pengurang pada perhitungan Masa Pajak Terakhir).*

### 6.3 Mekanisme Pemotongan Masa Pajak Biasa (Januari–November) (`BR-TAX-003`) & Multi-Run (`BR-TAX-009`)
- Berlaku untuk pegawai tetap yang masih aktif hingga akhir masa pajak bulan berjalan (bukan bulan berhenti bekerja).
- **Formula Tunggal (`BR-TAX-003`):**
  `PPh 21 Masa = FLOOR(Bruto Pajak Sebulan × Tarif_TER(Kategori_TER, Bruto Pajak Sebulan))`
- **Aturan Multi-Run dalam Satu Masa Pajak (`BR-TAX-009`):**
  Apabila terdapat lebih dari satu `PayrollRun` yang dibayarkan pada bulan kalender yang sama (mis. `THR Run` tgl 10 dan `Regular Run` tgl 25):
  1. Hitung **`Total_Bruto_Bulan = Bruto_Run_Saat_Ini + Σ Bruto_Run_Sebelumnya_Bulan_Ini`**
  2. Tentukan tarif TER menggunakan `Total_Bruto_Bulan`: `PPh_Bulan_Seharusnya = FLOOR(Total_Bruto_Bulan × Tarif_TER(Kategori_TER, Total_Bruto_Bulan))`
  3. **`PPh_Run_Saat_Ini = PPh_Bulan_Seharusnya − Σ PPh_21_Sudah_Dipotong_Bulan_Ini`**
- Tabel batas penghasilan dan tarif `TER A`, `TER B`, dan `TER C` (berakhir hingga tarif maksimal **34%**, dengan batas 0% misalnya hingga Rp5.400.000 untuk TER A) **wajib diimpor langsung dari Lampiran resmi PP 58/2023 dan PMK 168/2023** ke tabel `tax_ter_brackets` per `LegalRuleVersion`.
- Validasi status pemadanan NIK–NPWP (Coretax) mengikuti ketentuan DJP yang berlaku; setiap aturan tambahan tarif tanpa NPWP/NIK valid dikonfirmasi langsung oleh Tax Officer ke peraturan pelaksana DJP.

### 6.4 Mekanisme Masa Pajak Terakhir — Desember atau Bulan Berhenti (`BR-TAX-004`)
Pada bulan Desember (untuk pegawai aktif) atau bulan terakhir bekerja (untuk pegawai resign/PHK/habis kontrak di tengah tahun):
1. **Akumulasi Bruto Setahun (`Bruto_Tahun`):** Jumlahkan seluruh `Bruto Pajak Sebulan` dari masa pajak pertama dalam tahun berjalan hingga masa pajak terakhir. *(Catatan: Jika pegawai merupakan pindahan dalam perusahaan yang sama atau memiliki bukti potong A1/BPA1 dari pemberi kerja sebelumnya bila relevan sesuai kondisi pajak, catat pada konfigurasi profil pajak).*
2. **Hitung Pengurang Setahun:**
   - **Biaya Jabatan:** `MIN(5% × Bruto_Tahun, 500.000 × Jumlah_Bulan_Bekerja_Tahun_Ini)` dengan batas maksimal **Rp6.000.000 per tahun**.
   - **Iuran Pensiun/THT Pekerja:** Akumulasi `JHT Pekerja (2%) + JP Pekerja (1%)` yang dibayar pegawai selama bulan aktif bekerja dalam tahun berjalan.
3. **Penghasilan Neto:** `Neto = Bruto_Tahun - Biaya_Jabatan - Total_JHT_JP_Pekerja`. *(Disetahunkan hanya untuk kondisi khusus sesuai PMK 168/2023 seperti WNA/ekspatriat masuk/meninggalkan Indonesia atau karyawan meninggal dunia).*
4. **PKP (Penghasilan Kena Pajak):** `PKP = MAX(0, FLOOR_TO_THOUSAND(Neto - PTKP_Setahun))` *(dibulatkan ke bawah ke ribuan penuh)*.
5. **Hitung PPh 21 Terutang Setahun (`PPh_Setahun`) dengan Tarif Pasal 17 UU HPP:**
   - Lapis 1: `0 s/d Rp 60.000.000` -> **5%**
   - Lapis 2: `> Rp 60.000.000 s/d Rp 250.000.000` -> **15%**
   - Lapis 3: `> Rp 250.000.000 s/d Rp 500.000.000` -> **25%**
   - Lapis 4: `> Rp 500.000.000 s/d Rp 5.000.000.000` -> **30%**
   - Lapis 5: `> Rp 5.000.000.000` -> **35%**
6. **Hitung Selisih Masa Pajak Terakhir:**
   `PPh_Masa_Terakhir = PPh_Setahun - Σ PPh_Yang_Telah_Dipotong_Masa_Sebelumnya`
   - Jika `PPh_Masa_Terakhir >= 0` -> dipotong dari gaji bulan terakhir (**Kurang Potong**).
   - Jika `PPh_Masa_Terakhir < 0` -> kelebihan potong (**Lebih Potong**) **wajib dikembalikan** ke penghasilan pegawai pada bulan tersebut (untuk skema Gross) atau dicatat sesuai metode penanggungan perusahaan.

### 6.5 Metode Gross, Gross-Up & Net (`BR-TAX-005`)

| Metode | Perlakuan pada Slip Gaji & Pajak | Logika Engine |
|---|---|---|
| **GROSS** | Pajak ditanggung karyawan. | `Net Pay` berkurang sebesar `PPh 21`. |
| **NET** | Pajak ditanggung perusahaan sebagai tunjangan pajak/beban tanpa menambah bruto karyawan atau ditanggung sesuai skema pajak final/koreksi fiskal perusahaan. | `PPh 21` dihitung dari bruto tanpa tunjangan pajak; dicatat sebagai *Employer Tax Expense* (tidak mengurangi *Net Pay* karyawan). *Catatan: sesuai ketentuan pajak, pajak ditanggung pemberi kerja tanpa masuk tunjangan pajak umumnya merupakan kenikmatan/koreksi fiskal PPh Badan — konfirmasi ke Tax Officer.* |
| **GROSS-UP** | Perusahaan memberikan **Tunjangan Pajak** yang nilainya sama persis dengan PPh 21 yang terutang, sehingga *Net Pay* karyawan utuh dan tunjangan pajak masuk sebagai penghasilan bruto yang dapat dibiayakan perusahaan. | **Algoritma Iterasi Fixed-Point:**<br>1. Set `T_0 = 0`<br>2. Hitung `PPh_i = HitungPPh(Bruto_Dasar + T_i)`<br>3. Jika `|PPh_i - T_i| <= 1` (konvergen), stop; jika tidak, set `T_{i+1} = PPh_i` dan ulangi (maks 50 iterasi, pastikan penanganan kasus batas antar-lapisan TER/Pasal 17 agar tidak osilasi).<br>4. Masukkan `Tunjangan Pajak = PPh_final` ke komponen Earning dan `PPh 21 = PPh_final` ke Deduction. |

### Contoh Alur Perhitungan `BR-TAX-003` & `BR-BPJS` (Terintegrasi dengan Trace JSON `03-ERD`):
- Karyawan status `TK/0` (**Kategori TER A**), Gaji Pokok Rp10.000.000, BPJS Kesehatan Pemberi Kerja 4% (`Rp400.000`), JKK Kelas I 0,24% (`Rp24.000`), JKM 0,30% (`Rp30.000`).
- **Bruto Pajak Sebulan (Gross):** `10.000.000 + 400.000 + 24.000 + 30.000 = Rp10.454.000` *(tanpa menambahkan iuran JHT 3,7%, JP 2%, maupun JKP)*.
- **PPh 21 Masa Biasa:** `FLOOR(10.454.000 × Tarif_TER_A_Resmi(10.454.000))` dimana `Tarif_TER_A_Resmi` dibaca secara deterministik dari tabel `tax_ter_brackets` hasil impor Lampiran resmi **PP 58/2023**. *(Catatan: Nilai persentase TER eksak wajib diambil dari tabel lampiran resmi saat seeding `LegalRuleVersion`, bukan angka ilustrasi manual).*

---

## 7. Aturan Perhitungan THR Keagamaan (`BR-THR` — Permenaker 6/2016)

| Rule ID | Nama Aturan | Formula & Ketentuan |
|---|---|---|
| **BR-THR-001** | **Syarat Kelayakan Masa Kerja** | Karyawan (PKWTT & PKWT) berhak atas THR apabila pada **tanggal Hari Raya Keagamaan** memiliki masa kerja minimal **1 (satu) bulan secara terus-menerus**. |
| **BR-THR-002** | **Formula THR Penuh vs Proporsional** | - Jika `Masa Kerja >= 12 bulan`: **`THR = 1 × Upah Sebulan`**<br>- Jika `1 bulan <= Masa Kerja < 12 bulan`: **`THR = (Masa Kerja dalam Bulan / 12) × Upah Sebulan`**<br>*(Dimana `Upah Sebulan = Gaji Pokok + Tunjangan Tetap` pada tanggal acuan. Kebijakan pembulatan masa kerja hari ke bulan mengikuti kebijakan tertulis perusahaan yang tidak merugikan pekerja).* |
| **BR-THR-003** | **Kasus Resign / PHK PKWTT Mendekati Hari Raya** | Sesuai Pasal 7 Permenaker 6/2016: Pekerja **PKWTT** yang hubungan kerjanya berakhir (PHK oleh pengusaha) sejak **30 hari sebelum Hari Raya Keagamaan** tetap berhak atas THR untuk tahun berjalan. Pekerja PKWT yang berakhir kontraknya sebelum Hari Raya tidak berhak kecuali diatur lebih baik oleh kebijakan perusahaan. |
| **BR-THR-004** | **Batas Waktu Pembayaran & Pajak** | Pembayaran THR wajib dilakukan paling lambat **7 hari sebelum Hari Raya (H-7)**. Nilai THR masuk ke penghasilan bruto pajak pada masa pajak saat THR dibayarkan (`BR-TAX-002`). |

---

## 8. Aturan Offboarding, Kompensasi PKWT & PHK (`BR-FINAL` — PP 35/2021)

| Rule ID | Nama Aturan | Formula & Ketentuan |
|---|---|---|
| **BR-FINAL-001** | **Uang Kompensasi Berakhirnya PKWT** | Pekerja PKWT dengan masa kerja minimal **1 bulan secara terus-menerus** yang kontraknya berakhir (atau selesai diperpanjang, per tahap kontrak) berhak atas Uang Kompensasi.<br>**Formula (PP 35/2021 Pasal 16):**<br>`Uang Kompensasi = (Masa Kerja Kontrak dalam Bulan / 12) × 1 Bulan Upah` *(Upah = Gaji Pokok + Tunjangan Tetap)*.<br>*Catatan:* Jika kontrak berakhir lebih awal oleh salah satu pihak, hitung berdasarkan jangka waktu kontrak yang telah dilaksanakan. Tidak berlaku bagi WNA. |
| **BR-FINAL-002** | **Tabel Skala Dasar UP & UPMK (PP 35/2021 Pasal 40)** | **Uang Pesangon (UP) Dasar:**<br>- `< 1 thn`: 1 bulan upah<br>- `1 s/d < 2 thn`: 2 bulan upah<br>- `2 s/d < 3 thn`: 3 bulan upah<br>- `3 s/d < 4 thn`: 4 bulan upah<br>- ... (bertambah 1 bulan tiap tahun hingga `>= 8 thn`: **9 bulan upah**).<br>**Uang Penghargaan Masa Kerja (UPMK) Dasar:**<br>- `3 s/d < 6 thn`: 2 bulan upah<br>- `6 s/d < 9 thn`: 3 bulan upah<br>- ... (bertambah 1 bulan tiap 3 tahun hingga `>= 24 thn`: **10 bulan upah**). |
| **BR-FINAL-003** | **Decision Table Pengali PHK** | Pengali akhir (`0,5x / 1x / 1,75x / 2x UP`, `1x UPMK`, serta `UPH` seperti sisa cuti tahunan & ongkos pulang) bergantung pada **Kode Alasan PHK** dalam PP 35/2021 Pasal 41–59 (mis. Efisiensi merugi vs tidak merugi, Pensiun, Meninggal Dunia, Sakit berkepanjangan, Pelanggaran mendesak, Mengundurkan diri). **Sistem wajib menyajikan rincian alasan & mewajibkan approval Legal/HR Manager sebelum kalkulasi final dikunci.** |
| **BR-FINAL-004** | **Pajak Final Pesangon (PP 68/2009)** | Uang Pesangon yang dibayarkan sekaligus (atau dalam rentang maks 2 tahun takwim) dikenakan **PPh 21 Final tersendiri** (0% s/d Rp50jt, 5% >50jt–100jt, 15% >100jt–500jt, 25% >500jt) dan **tidak digabung** dengan penghasilan gaji reguler/TER. Sedangkan Uang Kompensasi PKWT dan UPH sisa cuti mengikuti ketentuan pajak penghasilan teratur/tidak teratur yang berlaku sesuai konfirmasi Tax Officer. |

---

## 9. Aturan Pinjaman, Potongan Sah & Exception Blocking (`BR-EXC`)

| Rule ID | Nama Aturan | Tingkat Keparahan | Kondisi & Aksi Sistem |
|---|---|---|---|
| **BR-EXC-001** | **Urutan Prioritas Potongan** | Sistem Rule | Urutan pemotongan wajib: (1) PPh 21, (2) Iuran BPJS Pekerja wajib, (3) Cicilan Pinjaman/Kasbon Perusahaan jatuh tempo, (4) Potongan sukarela/lainnya yang disetujui tertulis. |
| **BR-EXC-002** | **Larangan Net Pay Negatif** | **BLOCKER** | Jika `Total Employee Deductions > Gross Earnings` (sehingga `Net Pay < 0`), blokir persetujuan payroll untuk karyawan tersebut, tahan potongan urutan ke-3/ke-4 (atau jadwalkan ulang cicilan pinjaman) setelah verifikasi HR/Finance. |
| **BR-EXC-003** | **Batas Maksimum Potongan Non-Wajib** | **WARNING / BLOCKER** | Mengacu aturan pengupahan (PP 36/2021 Pasal 65), total potongan upah di luar pajak & iuran wajib (seperti cicilan/denda/ganti rugi) tidak boleh melebihi **50% dari setiap pembayaran upah** (kecuali saat *Final Settlement* dengan surat persetujuan tertulis karyawan). |
| **BR-EXC-004** | **Missing Rule-Set Version** | **BLOCKER** | Jika pada tanggal periode berjalan tidak ditemukan versi tabel TER, batas upah BPJS, atau UMP/UMK yang aktif, gagalkan proses kalkulasi dan tampilkan pesan error konfigurasi. |
| **BR-EXC-005** | **Rekening Bank Tidak Valid / Berubah < H-5** | **PAYMENT HOLD** | Karyawan tetap masuk perhitungan payroll & jurnal akrual, namun `PaymentInstruction` ditahan (`On Hold`) dan tidak dimasukkan ke file transfer bank hingga diverifikasi Finance Manager. |
| **BR-EXC-006** | **Anomali Lonjakan Gaji / Variance** | **WARNING** | Jika `Gross Earnings` atau `Net Pay` berubah > ±20% dibanding periode sebelumnya tanpa disertai catatan promosi, lembur tinggi, atau bonus/THR, sistem menandai karyawan pada laporan *Variance Review* untuk wajib diperiksa Payroll Supervisor. |
| **BR-EXC-007** | **Ketidaksinkronan Absensi Cutoff** | **BLOCKER / POLICY** | Jika masih ada `AttendanceException` berstatus *Open/Unresolved* pada saat pembuatan snapshot payroll, sistem memblokir run (atau menerapkan *unpaid absence* sesuai parameter kebijakan yang disetujui HR). |
| **BR-EXC-008** | **Imutabilitas Run Approved** | **HARD SYSTEM BLOCK** | Setiap permintaan ubah/hapus terhadap record `PayrollRun`, `PayrollResult`, `PayrollDetail`, atau `TaxCalculation` milik run berstatus `Approved`/`Paid` ditolak oleh sistem; wajib lewat run baru (`Adjustment`/`Off-Cycle`/`Reversal`). |
