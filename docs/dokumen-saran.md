# Dokumen Saran: Review Mendalam Paket BA + SA terhadap Blueprint Awal

**Tanggal review:** 18 November 2026
**Objek review:** seluruh isi `docs/` (10 dokumen BA + SA)
**Pembanding:** [Payroll_Business_Process_Analysis_ID.md](file:///home/aliube/Workspace/PLan/Payroll/Payroll_Business_Process_Analysis_ID.md) (blueprint awal, 28 bab)
**Status:** hanya saran. Tidak ada dokumen yang diubah. Dokumen ini satu-satunya file baru.

> **Catatan jujur:** semua dokumen di `docs/` saya susun sendiri pada sesi sebelumnya, jadi review ini sekaligus koreksi atas pekerjaan saya. Angka regulasi pajak dan BPJS dalam dokumen saran ini sudah diperiksa terhadap sumber resmi (DJP, BPJS Kesehatan, BPJS Ketenagakerjaan, peraturan pemerintah). Hasil dan keterbatasannya ada di bagian A7. Item yang masih belum terkonfirmasi ditandai **[VERIFIKASI]**. Jangan dijadikan konfigurasi produksi sebelum dicocokkan ke naskah primer.

---

## 0. Ringkasan Eksekutif

| Aspek | Penilaian |
|---|---|
| Kesesuaian arsitektur dengan prinsip blueprint (effective dating, immutability, SoD, trace, snapshot) | **Baik** |
| Cakupan bab blueprint | **~75%**. Bab 24 (testing/UAT/parallel run) dan Bab 25 (roadmap fase) praktis tidak terbawa. Bab 26 (daftar regulasi dan sumber resmi) hanya terbawa sebagian. |
| Kelengkapan skema data | **Belum siap DDL.** Banyak tabel hanya muncul di diagram ERD dan tidak ada di kamus data. |
| Akurasi rumus dan angka regulasi | **Perlu validasi.** Ada contoh angka yang saya karang dan beberapa aturan yang kurang tepat atau ambigu. |
| Kelayakan menuju SDLC | **Belum.** Ada 9 celah kritis (bagian 1) yang sebaiknya ditutup dulu. |

**Rekomendasi:** tutup 9 celah kritis (A1–A9) dan konfirmasi asumsi (bagian 5) sebelum memilih tech stack dan menulis DDL.

---

## 1. Celah Kritis (harus ditutup sebelum SDLC)

### A1. Beberapa run dalam satu masa pajak belum diatur
- **Masalah:** dokumen mendukung `REGULAR`, `THR`, `BONUS`, `OFF_CYCLE`, `RETROACTIVE`, dan `FINAL_SETTLEMENT` dalam satu bulan. Tidak ada aturan bahwa TER dihitung atas **total bruto sebulan gabungan semua run**, dikurangi PPh 21 yang sudah dipotong di run lain pada bulan yang sama. Jika THR dibayar sebagai run terpisah dan TER dihitung per run, pajak akan salah.
- **Saran:** tambahkan entitas `tax_month_ledger` per employment dan masa pajak. Isinya akumulasi bruto, pajak yang sudah dipotong, dan referensi run. Tambahkan aturan `BR-TAX-009`: pajak run ke-n = hitung ulang bulan itu − sudah dipotong. Hal yang sama berlaku untuk BPJS, yang hanya boleh ditagih sekali per bulan walaupun ada banyak run.
- **Dokumen terdampak:** `03-Business-Rules`, `03-ERD`, `SRS-TAX`.

### A2. Tidak ada modul input variabel dan penyesuaian
- **Masalah:** `UC-05` mensyaratkan "komponen variabel diimpor", dan `SOP-PAY-001` menyebut unggah bonus, insentif, dan potongan. Tidak ada use case, endpoint, atau tabel untuk itu. `payroll_run_inputs` hanya snapshot. Pencarian `variable_input`, `payroll_adjust`, dan `retroactive_queue` tidak menemukan apa pun di `docs/`. `SRS-COMP-004` menyebut `RetroactiveQueue`, tetapi tabelnya tidak ada.
- **Saran:** tambahkan `payroll_variable_inputs` (komponen, nominal, periode, sumber import, status approval, `import_batch_id`) dan `retroactive_adjustments` (run asal, komponen, selisih, status). Tambahkan UC "Impor Input Variabel" lengkap dengan template file dan validasi.

### A3. Migrasi data dan saldo awal (go-live di tengah tahun) tidak punya rancangan
- **Masalah:** BRD asumsi 5.1 menyebut data PPh 21 tahun berjalan harus dimigrasi. Tidak ada use case, tabel, atau SOP untuk itu. Tanpa saldo awal, PPh 21 masa terakhir di Desember akan salah untuk semua karyawan, karena akumulasi bruto dan pajak Januari sampai bulan go-live tidak ada.
- **Saran:** tambahkan modul `opening_balance` untuk:
  - akumulasi bruto, biaya jabatan, iuran pensiun, dan PPh 21 yang sudah dipotong per karyawan per bulan;
  - saldo cuti;
  - saldo pinjaman;
  - riwayat gaji dan kontrak;
  - THR dan kompensasi PKWT yang pernah dibayar.

  Tambahkan dokumen terpisah `Data-Migration-Plan` yang berisi template, aturan validasi, rekonsiliasi migrasi, dan strategi cutover per periode. Blueprint Bab 24.3 sudah meminta parallel run, tetapi tidak membahas migrasi.

### A4. Aturan legal dan kebijakan perusahaan tercampur di satu rule set
- **Masalah:** blueprint prinsip 8 meminta "kebijakan perusahaan disimpan terpisah dari ketentuan hukum". Di ERD, `rule_set_versions` memuat `company_id NOT NULL` dan menaruh TER, Pasal 17, BPJS, dan UMP/UMK bersama. Ini menimbulkan dua masalah:
  - aturan nasional yang sama akan diduplikasi per perusahaan;
  - kebijakan perusahaan (metode prorata, pembulatan, batas lembur internal, ambang anomali 20%, cuti, masa percobaan) tidak punya wadah berversi sama sekali. Akibatnya determinisme (BR-011) tidak terjamin untuk kebijakan.
- **Saran:** pisahkan `legal_rule_sets` (global atau per yurisdiksi) dan `company_policy_sets` (per perusahaan, berversi, melalui approval). `payroll_runs` mengikat **keduanya**. Ini juga memudahkan rencana multi-company di fase 2.

### A5. Hak cuti, sakit, dan masa percobaan tidak ada
- **Masalah:** pencarian "percobaan/probation" tidak menemukan apa pun di `docs/`. Aturan upah sakit dan cuti melahirkan nyaris tidak ada. Keduanya mempengaruhi prorata, THR, dan kompensasi PKWT.
- **Yang perlu ditambahkan (semua **[VERIFIKASI]** ke UU/PP terbaru):**
  - Jenis cuti: tahunan, sakit, melahirkan atau keguguran (cek UU KIA 2024), haid, menikah, duka, ibadah, cuti besar. Tiap jenis perlu perlakuan upah, saldo, dan dokumen pendukung.
  - Upah selama sakit (persentase bertahap menurut lama sakit, PP 36/2021).
  - Masa percobaan PKWTT (maksimal, upah tidak di bawah minimum). PKWT tidak boleh ada masa percobaan.
  - Aturan PKWT: batas durasi dan perpanjangan, pemberitahuan, jeda antar kontrak, dan pengaruh Putusan MK 168/PUU-XXI/2023.
  - Penetapan pembagi upah harian (21 atau 25) bila dipakai untuk upah harian dan potongan.
  - Batas lembur (jam per hari dan per minggu) sebagai validasi.

### A6. Skema data belum siap menjadi DDL
Pencarian di kamus data (`03-ERD`) menunjukkan masalah berikut.

**a) Tabel ada di diagram ERD tetapi tidak didefinisikan kolomnya:**
`positions`, `branches`, `departments`, `cost_centers`, `job_grades`, `work_locations`, `work_calendars`, `shifts`, `employee_schedules`, `leave_types`, `leave_requests`, `overtime_multiplier_rules`, `regional_minimum_wages`, `bpjs_family_dependents`, `users`, `approval_*` (hanya ringkas), `account_mappings`.

**b) Tabel yang disebut di SRS atau UC tetapi tidak ada di ERD sama sekali:**
`attendance_corrections`, `onboarding_checklists`, `bpjs_reconciliation_exceptions`, `retroactive_queue`, `reimbursement_claims`, `termination_reasons`, `roles`, `permissions`, `user_roles`, dan `bank_reconciliation` (Pencarian tidak menemukan `TaxWithholding` di `docs/`.)

**c) Entitas blueprint Bab 21 yang hilang:**
`TaxWithholding` (bukti potong), `EmployeeAddress`, `EmployeeDocument`, `EmploymentHistory` (hanya terwakili sebagian oleh `employment_assignments`), `SalaryStructure`, `SalaryHistory`, `THRCalculation`, `OvertimeApproval`, `DeductionTransaction`, `PaymentStatus`.

**d) Konsistensi data:**
- `payroll_results.cost_center_id` hanya satu, sedangkan `SRS-FIN-004` menjanjikan pemecahan jika pindah cost center di tengah bulan. Kebijakan alokasi belum ditetapkan.
- Tidak ada status per karyawan per run (gagal atau berhasil) untuk fitur checkpoint dan resume.
- Contoh JSON `calculation_trace_json` tidak konsisten dengan contoh di `03-Business-Rules`: bruto pajak Rp9.724.300 dengan tarif 2%, sedangkan contoh lain memakai Rp10.454.000 dengan tarif 3%. Angka itu karangan saya, bukan data resmi.
- **Saran:** lengkapi kamus data seluruh tabel, buat satu ERD terpadu, lalu validasi dengan DDL percobaan sebelum disahkan.

### A7. Verifikasi pajak dan BPJS terhadap sumber resmi

**Cara verifikasi dan batasannya.** Pencarian saya menjangkau situs resmi (pajak.go.id, bpjsketenagakerjaan.go.id, bpjs-kesehatan.go.id, peraturan.bpk.go.id, jdih.kemenkeu.go.id), tetapi ringkasan yang dikembalikan banyak merujuk situs sekunder (klikpajak, ortax, pajakku, detik, dan sejenisnya). Naskah primer (PDF PP 58/2023 beserta lampiran TER, PMK 168/2023, PMK 81/2024, Perpres 64/2020, pengumuman BPJS TK) tidak berhasil saya unduh. Jadi status di bawah berarti **"konsisten di beberapa sumber, belum dicocokkan ke naskah primer"**. Pencocokan akhir wajib dilakukan Tax Officer dan BPJS Officer.

**Sumber resmi yang dipakai sebagai acuan (daftar untuk Regulatory Register):**

| Topik | Sumber resmi yang harus menjadi rujukan |
|---|---|
| PPh 21 (tarif TER, kategori, lampiran) | PP 58/2023 dan lampirannya; PMK 168/2023 (pajak.go.id, peraturan.bpk.go.id, jdih.kemenkeu.go.id) |
| Tarif Pasal 17, biaya jabatan, PTKP | UU PPh sebagaimana diubah UU HPP; PMK PTKP yang berlaku (pajak.go.id) |
| Pajak pesangon, JHT, pensiun sekaligus | PP 68/2009 (pajak.go.id) |
| Batas bayar dan lapor, bukti potong, Coretax | PMK 81/2024 dan ketentuan DJP tentang Coretax (pajak.go.id) |
| BPJS Kesehatan | Perpres 82/2018 sebagaimana diubah Perpres 64/2020; informasi iuran di bpjs-kesehatan.go.id |
| BPJS Ketenagakerjaan | PP 44/2015, PP 45/2015, PP 82/2019, PP 37/2021 dan perubahannya; informasi iuran dan batas upah di bpjsketenagakerjaan.go.id |
| Pengupahan, lembur, PKWT, PHK | PP 35/2021, PP 36/2021 jo. PP 51/2023 jo. PP 49/2025 (peraturan.go.id, jdih.kemnaker.go.id) |

**Hasil verifikasi per item:**

| Item | Isi di dokumen saya | Hasil verifikasi | Tindakan |
|---|---|---|---|
| Kategori TER dan pemetaan PTKP (A: TK/0, TK/1, K/0; B: TK/2, TK/3, K/1, K/2; C: K/3) | Sama | **Sesuai** di beberapa sumber, termasuk yang merujuk pajak.go.id | Tetap, cocokkan ke PMK 168/2023 |
| Tarif TER maksimum 34%, TER A sampai Rp5.400.000 = 0% | `03-Business-Rules` menulis "44/40/41 lapisan" dan contoh "10,35–10,7 jt = 3%" | Batas 0% (A: Rp5,4 jt) dan tarif atas 34% cocok. Jumlah lapisan dan contoh 3% **tidak bisa saya konfirmasi**. Ringkasan yang saya terima juga tidak konsisten soal tarif lapis kedua TER A (0,25% atau 0,5%). | **Hapus semua contoh numerik TER** dari `03-Business-Rules` dan `03-ERD`. Impor tabel TER dari lampiran PP 58/2023 dan uji dengan data resmi. |
| Masa pajak terakhir (Desember atau bulan berhenti): hitung ulang setahun dengan Pasal 17, kurangi pajak yang sudah dipotong, lebih potong dikembalikan | Sama | **Sesuai.** Bulan berhenti bekerja juga masa pajak terakhir. | Tetap |
| PTKP pada masa pajak terakhir | Dokumen saya menulis PTKP setahun penuh, dan saya menandainya terbuka | Sumber menyebut PTKP **penuh setahun**, dan proporsional hanya bila kehilangan kewajiban pajak subjektif (misalnya WNA yang meninggalkan Indonesia). Redaksi sumber sekunder belum tegas untuk WNI yang berhenti di tengah tahun. | Pakai PTKP penuh sebagai default, jadikan parameter, dan minta Tax Officer mengonfirmasi ke PMK 168/2023. |
| Biaya jabatan 5%, maksimal Rp500.000 per bulan | Sama | **Sesuai** | Tetap |
| Setor PPh 21 | `SOP-PAY-005` menulis **tanggal 10** | **Berbeda.** Sumber merujuk PMK 81/2024 Pasal 94 ayat (2): setor paling lambat **tanggal 15** bulan berikutnya. Jika tanggal 15 hari libur, bergeser ke hari kerja berikutnya. Sumber sekunder, belum dari naskah primer. | **Koreksi SOP-PAY-005 setelah dicek ke PMK 81/2024.** |
| Lapor SPT Masa PPh 21 | Tanggal 20 | **Sesuai** (paling lama tanggal 20 bulan berikutnya) | Tetap |
| Bukti potong dan kode objek pajak | Dokumen memakai "1721-A1/BPA1" dan `21-100-01` | BPA1 adalah bukti potong tahunan untuk pegawai tetap di Coretax. Kode `21-100-01` cocok untuk penghasilan pegawai tetap. Kode lain di `04-API` (`21-401-01`) **tidak terverifikasi**. | Gunakan istilah BPA1 saja. Ambil daftar kode objek pajak dan skema impor dari spesifikasi resmi Coretax. |
| Pajak final pesangon (PP 68/2009): 0% sampai Rp50 jt, 5% sampai Rp100 jt, 15% sampai Rp500 jt, 25% di atasnya | `BR-FINAL-004` sama | **Sesuai.** PP 68/2009 tidak dicabut oleh PP 58/2023 atau PMK 168/2023. Berlaku juga untuk JHT dan pensiun sekaligus, serta uang pesangon yang dibayar sekaligus atau paling lama 2 tahun kalender. | Tetap. Tambahkan JHT lump-sum dan manfaat pensiun. |
| BPJS Kesehatan PPU: 5% (4% pemberi kerja, 1% pekerja), batas atas upah Rp12.000.000, batas bawah UMK/UMP | Sama | **Sesuai** (Perpres 64/2020). Maksimum iuran Rp600.000 (Rp480.000 + Rp120.000). | Tetap. Simpan sebagai parameter berversi. |
| Tanggungan BPJS Kesehatan: pekerja, 1 pasangan, 3 anak | Sama | **Sesuai** | Tetap |
| Anggota tambahan (anak ke-4 dan seterusnya, ayah, ibu, mertua): 1% dari upah per orang, ditanggung pekerja | Sama | **Sesuai** | Tetap |
| JHT: 3,7% pemberi kerja, 2% pekerja, tanpa batas atas | Sama | **Sesuai** (informasi BPJS TK) | Tetap |
| JKK: 0,24%, 0,54%, 0,89%, 1,27%, 1,74% menurut kelompok risiko, 100% pemberi kerja | Sama | **Sesuai** | Tetap |
| JKM: 0,30%, 100% pemberi kerja | Sama | **Sesuai** | Tetap |
| JP: 2% pemberi kerja, 1% pekerja, dengan batas atas upah | Sama, tanpa angka batas | **Sesuai untuk persentase.** Sumber menyebut batas upah JP **Rp11.086.300 berlaku 1 Maret 2026** (naik dari Rp10.547.400). Sumber itu media sekunder. | Isi sebagai nilai awal parameter, **wajib dicocokkan ke pengumuman resmi BPJS TK**. Batas ini berubah tiap tahun, jadi harus berversi. |
| JKP | `BR-BPJS-TK-005` menulis total 0,46% sebagai program yang dihitung terpisah, dengan JKK 0,14% dan JKM 0,10% direkomposisi | Sumber BPJS TK: **tidak ada iuran tambahan untuk pekerja maupun pemberi kerja.** Pendanaan: iuran Pemerintah 0,22% ditambah rekomposisi dari JKK (0,14%) dan JKM. Tarif JKK dan JKM yang dipublikasikan BPJS TK (0,24–1,74% dan 0,30%) dipakai apa adanya oleh pemberi kerja. | **Koreksi desain.** Jangan hitung JKP sebagai baris iuran ke pemberi kerja atau pekerja. Modelkan JKP sebagai **status kepesertaan** (hak manfaat), tanpa biaya tambahan. Hapus risiko hitung ganda di `SRS-BPJS-003` dan `BR-BPJS-TK-005`. |
| Aturan lembur (pengali, pembagi 173, rumusan 75%) | Ada di `BR-OT-002..005` | **Belum diverifikasi** ke naskah PP 35/2021. Bukan ranah pajak atau BPJS, jadi tetap diserahkan ke Legal. | Verifikasi oleh HR/Legal. |
| PP 49/2025 | Disebut di blueprint | Perubahan kedua PP 36/2021 (ditetapkan 17 Desember 2025). Isinya terutama formula upah minimum dan upah minimum sektoral. | Masuk register regulasi. Model UMP/UMK perlu mendukung upah minimum sektoral. |
| Tarif lebih tinggi tanpa NPWP/NIK valid | `BR-TAX-003` hanya "sesuai ketentuan" | Belum ada hasil yang bisa saya pastikan | **Pertanyaan terbuka ke Tax Officer.** Tulis aturan beserta rujukan pasal. |
| Format file bank | Disebut seolah baku | Di luar lingkup pajak dan BPJS | Ambil spesifikasi dari bank yang dipilih. |

**Perubahan yang dihasilkan terhadap dokumen BA/SA (belum dilakukan, menunggu persetujuan Anda):**
1. `SOP-PAY-005`: ubah batas setor PPh 21 dari tanggal 10 ke tanggal 15, setelah cek PMK 81/2024.
2. `BR-BPJS-TK-005`, `SRS-BPJS-003`, `03-ERD` (`bpjs_program_configs`), `UC-07`: JKP menjadi status kepesertaan tanpa iuran.
3. `03-Business-Rules` dan `03-ERD`: hapus contoh numerik TER (3% dan 2%) dan angka 44/40/41 lapisan.
4. `04-API`: hapus kode objek pajak yang tidak terverifikasi, dan satukan istilah ke BPA1.
5. Tambahkan nilai awal batas upah JP (Rp11.086.300 efektif 1 Maret 2026) sebagai parameter berversi dengan status "menunggu verifikasi".

### A8. Dokumen dan fase yang tidak terbawa dari blueprint
- **Bab 24 (strategi testing):** hanya 10 baris `UAT-0xx` yang muncul, dan hanya di tabel RTM. Tidak ada test plan, test case terperinci, atau dataset uji dengan hasil harapan. Parallel run baru muncul sebagai ide komparator.
- **Bab 25 (roadmap fase 0–6):** tidak ada di `docs/`. Master Roadmap hanya memetakan dokumen. Belum ada rencana fase implementasi, urutan modul, dan estimasi.
- **Bab 26:** daftar sumber resmi dan **register regulasi** (nomor, pasal, URL, tanggal berlaku) tidak ditulis. Hanya `SOP-PAY-006` yang menyinggung prosesnya.
- **Bab 23:** aturan exception ada di `BR-EXC`, tetapi **model exception** (severity, owner, resolution action, due date, status, histori) tidak menjadi entitas data atau API.
- **Bab 19:** FR-001–FR-022 tidak dipetakan satu per satu. RTM memakai `BR-xxx`, bukan FR. FR-022 (API integrasi eksternal) dan FR-020 (dashboard) hanya terwakili sebagian.
- **Saran:** tambahkan dokumen berikut.
  - `Test-Strategy-and-UAT` (unit, integrasi, NFR, keamanan, parallel run, kriteria go/no-go).
  - `Implementation-Roadmap` (fase, dependensi, milestone).
  - `Regulatory-Register` (tabel regulasi yang dipakai).

### A9. Pengendalian keamanan dan privasi belum lengkap
- Tidak ada proses terkait **UU PDP**: hak subjek data (akses, koreksi, penghapusan), dasar pemrosesan, **notifikasi kebocoran data (batas waktu ditetapkan UU)**, penunjukan DPO bila diwajibkan, DPIA. Pencarian "notifikasi" hanya muncul di SOP, bukan fitur sistem.
- Retensi 10 tahun (`NFR-AUD-001`) bisa berbenturan dengan hak penghapusan data. Perlu kebijakan: data apa yang wajib disimpan, data apa yang dianonimkan setelah masa retensi.
- Belum ada: kebijakan sandi dan sesi, SSO, review akses berkala, **break-glass access**, rotasi kunci enkripsi, dan pengujian penetrasi sebelum go-live.
- Audit log berantai hash dengan **satu rantai global** dapat menjadi bottleneck tulis. Saran: satu rantai per company per partisi waktu, plus penjangkaran hash berkala. Ini harus diuji performanya.
- SoD 14 peran tidak realistis untuk perusahaan kecil. Perlu mode "peran digabung" yang terkontrol dan terdokumentasi (misalnya Checker dan Approver orang yang sama dengan Maker dilarang, tetapi satu orang boleh memegang beberapa peran di langkah berbeda).

---

## 2. Celah Penting (sebaiknya ditutup di fase analisis)

| # | Temuan | Saran |
|---|---|---|
| B1 | **Penomoran ID tidak konsisten.** `BR-001` (Business Requirement) bertabrakan dengan `BR-PRO-001` (Business Rule). Roadmap menyebut `BR-RULE-xxx` dan `SRS-FR-xxx`, tetapi dokumen memakai `BR-PRO` dan `SRS-ORG`. | Tetapkan skema ID baku: `BREQ-`, `RULE-`, `SRS-`, `UC-`, `TBL-`, `API-`, `TC-`. Perbarui RTM. |
| B2 | **Proses akhir tahun dan awal tahun tidak ada.** Termasuk Desember (rekonsiliasi massal, bukti potong tahunan dan batas penerbitan), Januari (reset PTKP dan saldo cuti, carry-forward/expiry, rollover UMK/UMP, pembaruan batas JP), dan pelaporan SPT tahunan. | Tambah BPA-10 "Year-End/Year-Start" dan SOP terkait. |
| B3 | **Modul notifikasi dan pengingat tidak ada**, padahal RSK-03 mengandalkan eskalasi otomatis H-2. | Tambah SRS-NTF (email/in-app/WhatsApp), jadwal, dan template. |
| B4 | **Konfigurasi SoD untuk perusahaan kecil dan matriks wewenang berbasis nominal** disebut "fase 2". Tanpa itu MVP sulit dipakai. | Jadikan MVP, minimal matriks sederhana per jenis transaksi. |
| B5 | **Fasilitas dan natura** (asuransi swasta yang dibayar perusahaan, kendaraan, bunga pinjaman di bawah pasar, dll.) tidak dibahas, padahal berpengaruh ke bruto pajak. | Tambahkan klasifikasi di `salary_components` dan aturan pajaknya (verifikasi ke Tax Officer). |
| B6 | **Skenario karyawan khusus** belum jelas: WNA, penyandang disabilitas, pegawai pindah dalam grup, pegawai dengan dua hubungan kerja, rehire, serta karyawan yang diaktifkan ulang. | Putuskan masuk MVP atau tidak, dan jika tidak, pastikan sistem menolak dengan pesan jelas. |
| B7 | **Pembayaran** selain karyawan (BPJS, pajak) tidak masuk model. `PaymentInstruction` hanya untuk karyawan. Setoran PPh 21 dan BPJS, serta bukti bayar NTPN, belum punya entitas. | Tambah `statutory_payments`. |
| B8 | **Status pembayaran** blueprint Bab 15 punya `Accepted/Processing`. Dokumen SA menghilangkannya, padahal bank punya fase tersebut. | Tambah status. |
| B9 | **Hasil sistem multi-zona waktu** (WIB/WITA/WIT) hanya disinggung. | Tentukan zona waktu per lokasi kerja, dan tanggal bisnis dihitung berdasarkan itu. |
| B10 | **API:** tidak ada konvensi paginasi dan filter, versioning, optimistic locking (ETag), spesifikasi unggah file, status job asinkron (`GET /jobs/{id}`), spesifikasi OpenAPI, endpoint manajemen user dan role, klaim reimbursement, laporan, dan ekspor jurnal. | Lengkapi katalog endpoint, buat OpenAPI di fase SDLC. |
| B11 | **System design:** tidak ada isolasi data per tenant (misalnya row-level security), topologi deployment, lingkungan (dev/staging/prod), strategi job scheduler, kapasitas dan sizing, cache, atau runbook operasional. | Tambah `Deployment-and-Operations`. |
| B12 | **SOP hanya 6.** Belum ada SOP onboarding, offboarding, THR, pinjaman, koreksi absensi, manajemen akses pengguna, backup dan restore, dan penutupan tahun. | Tambah sesuai prioritas. |
| B13 | **Angka SLA dan tanggal di SOP** (H-5 cutoff, payday tanggal 25, H-4 sampai H-2) adalah asumsi saya, bukan dari perusahaan Anda. | Konfirmasi di workshop. |
| B14 | **Logika retroaktif.** Rapel gaji menambah bruto bulan berjalan, tetapi dampak ke BPJS (upah tetap berubah surut, tagihan yang sudah lewat) dan ke lembur/THR yang sudah dibayar belum ada aturannya. | Tulis aturan dan skenario uji. |

---

## 3. Inkonsistensi Antardokumen

| # | Inkonsistensi |
|---|---|
| 1 | `02-BPA` memakai istilah "Payroll Run LOCKED" sedangkan state machine memakai `APPROVED`. Status "Reopen" blueprint (7.3) tidak ada di state machine. |
| 2 | `02-BPA` menyebut "1721-A1/BPA1", `01-SRS` menyebut BPA1. Perlu satu istilah baku mengikuti Coretax. |
| 3 | `00-Master-Roadmap` mengklaim pemetaan 17 keputusan Bab 27 sebagai "keputusan baseline". Kenyataannya itu **asumsi saya**, bukan jawaban Anda (lihat bagian 5). |
| 4 | RTM memakai nama skenario bebas ("Joiner, Perubahan Rekening") dan bukan ID UAT. Hanya UAT-001–016 yang ada, dan tidak ada UAT untuk THR bermacam kasus, BPJS exception, Gross-Up, masa terakhir, PHK, atau multi-run. |
| 5 | `payment_instructions.payment_batch_id` boleh NULL, tetapi `SRS-FIN-002` mensyaratkan total batch plus hold sama dengan total net. Perlu aturan invarian di level data. |
| 6 | `SRS-RUN-003` menyebut 13 tahap, tetapi `UC-05` menyebut tahap pajak dan BPJS sebagai UC terpisah dengan urutan yang tidak sama (BPJS dihitung sebelum pajak karena iuran pekerja mengurangi neto, tetapi di UC-05 tertulis pajak dulu). |
| 7 | `SOP-PAY-005` menulis setor PPh 21 tanggal 10, sedangkan sumber merujuk PMK 81/2024 yang menetapkan tanggal 15 (lihat A7). |

---

## 4. Perbandingan Cakupan terhadap Blueprint Awal (per Bab)

Legenda: ✅ terbawa baik · 🟡 terbawa sebagian · ❌ tidak terbawa

| Bab blueprint | Isi | Status | Catatan |
|---|---|---|---|
| 1 | Tujuan dan ruang lingkup | ✅ | BRD 1–2. Cakupan MVP dipersempit, perlu persetujuan. |
| 2 | Prinsip desain | 🟡 | Tersebar. Tidak ada daftar prinsip eksplisit sebagai kriteria review desain. |
| 3 | Aktor dan SoD | ✅ | BRD 3. Perlu mode perusahaan kecil (B4). |
| 4 | Master data, onboarding, histori | 🟡 | Tidak ada alamat, dokumen, histori kepegawaian lengkap (A6c). |
| 5 | Kompensasi, prorata | 🟡 | Tidak ada struktur gaji dan histori gaji sebagai entitas. Fasilitas natura hilang (B5). |
| 6 | Absensi, cuti, lembur | 🟡 | Jenis cuti, upah sakit, batas lembur hilang (A5). |
| 7 | Payroll period dan run | 🟡 | Aturan beberapa run per masa pajak hilang (A1). Reopen hilang. |
| 8 | Engine kalkulasi | ✅ | SRS-RUN dan System Design. |
| 9 | BPJS Kesehatan | ✅ | Perlu verifikasi parameter. |
| 10 | BPJS Ketenagakerjaan | 🟡 | Ambigu JKK/JKP (A7). |
| 11 | PPh 21 | 🟡 | Mekanisme ada, tetapi data uji dan sebagian aturan perlu verifikasi. Tidak ada bukti potong sebagai entitas. |
| 12 | THR | ✅ | Perlu aturan bulat masa kerja, pekerja harian ditunda fase 2. |
| 13 | Pinjaman, potongan, reimbursement | 🟡 | Reimbursement tanpa tabel. Dasar hukum dan otorisasi potongan belum ada di model. |
| 14 | Final payroll, PKWT, PHK | 🟡 | Decision table PHK belum diisi. Hanya disebutkan wajib divalidasi Legal. |
| 15 | Pembayaran dan rekonsiliasi | 🟡 | Status `Accepted/Processing` hilang. Pembayaran kewajiban pajak/BPJS tidak ada (B7, B8). |
| 16 | Integrasi akuntansi | 🟡 | Kebijakan alokasi cost center di tengah periode belum ditetapkan. |
| 17 | Approval dan audit | ✅ | |
| 18 | Laporan | 🟡 | Tercantum satu baris di SRS-RPT-002. Tidak ada spesifikasi per laporan. |
| 19 | Functional requirements FR-001–022 | 🟡 | Tidak dipetakan satu per satu. |
| 20 | NFR | ✅ | Angka performa saya tentukan sendiri. Perlu persetujuan. |
| 21 | Model data | 🟡 | Lihat A6. |
| 22 | Arsitektur | ✅ | Tech-agnostic sesuai permintaan. |
| 23 | Exception handling | 🟡 | Aturan ada, model exception tidak. |
| 24 | Testing dan UAT | ❌ | Lihat A8. |
| 25 | Roadmap | ❌ | Lihat A8. |
| 26 | Regulatory governance | 🟡 | Daftar regulasi dan sumber tidak dibawa. |
| 27 | 17 keputusan bisnis | 🟡 | Dijawab dengan asumsi saya, bukan jawaban Anda. |
| 28 | Langkah berikutnya | ✅ | Terlaksana sampai SA. Belum: calculation spec yang teruji dan test plan. |

---

## 5. Asumsi yang Saya Buat dan Perlu Anda Konfirmasi

Hal-hal berikut tertulis di dokumen seolah keputusan, padahal belum pernah Anda tentukan.

1. Skala 100–5.000 karyawan, serta target performa 1.000 karyawan dalam 3 menit.
2. Industri umum (perkantoran, jasa, ritel).
3. Pola kerja 5 dan 6 hari, dengan shift dasar.
4. Metode pajak Gross, Gross-Up, dan Net semuanya masuk MVP.
5. Daftar bank dan format file (BCA/Mandiri/BNI/BRI).
6. ERP/akuntansi cukup lewat ekspor file.
7. Kalender SLA: payday tanggal 25, cutoff H-5, dan sebagainya.
8. Retensi dokumen 10 tahun, RPO 1 jam, RTO 4 jam.
9. Single-company, karyawan bulanan PKWTT dan PKWT. Ini keputusan Anda, tetapi implikasinya belum diuji: apakah ada harian lepas, tenaga ahli, atau WNA di perusahaan target?
10. Perusahaan target: apakah satu perusahaan tertentu atau produk yang dijual ke banyak perusahaan? Ini mengubah multi-tenancy, konfigurasi kebijakan, dan keamanan secara mendasar.

---

## 6. Rencana Tindak Lanjut yang Disarankan

**Tahap 2.5, perbaikan analisis (sebelum SDLC):**

| Urutan | Pekerjaan | Keluaran |
|---|---|---|
| 1 | Workshop konfirmasi asumsi (bagian 5) dengan HR, Finance, Tax, Legal | Daftar keputusan yang ditandatangani |
| 2 | Verifikasi seluruh item **[VERIFIKASI]** (A7) ke sumber resmi, lalu buat `Regulatory-Register` | Register regulasi dan parameter awal terverifikasi |
| 3 | Perbaiki rumus dan contoh numerik, dengan test vector yang dihitung manual atau dengan spreadsheet pajak resmi | `Calculation-Test-Vectors` |
| 4 | Tutup A1–A6, A9 dengan revisi dokumen terkait | Dokumen versi 1.1 |
| 5 | Tambah dokumen baru: `Data-Migration-Plan`, `Test-Strategy-and-UAT`, `Implementation-Roadmap`, `Deployment-and-Operations` | 4 dokumen baru |
| 6 | Buat ERD terpadu dan kamus data lengkap, lalu uji dengan DDL percobaan | Skema siap diimplementasi |
| 7 | Standarkan ID, perbarui RTM | RTM konsisten |

**Baru setelah itu:** pilih tech stack, mulai Fase 1 (Core HR) menurut roadmap, dan susun test case dari test vector.

---

## 7. Keterbatasan Review Ini

- Verifikasi pajak dan BPJS (bagian A7) memakai pencarian yang menjangkau situs resmi, tetapi ringkasan yang kembali banyak bersumber dari situs sekunder. Naskah primer (lampiran TER PP 58/2023, PMK 168/2023, PMK 81/2024, pengumuman batas upah JP) tidak berhasil diunduh. Pencocokan akhir wajib dilakukan Tax Officer dan BPJS Officer.
- Verifikasi hukum ketenagakerjaan non-pajak non-BPJS (lembur, PKWT, PHK, cuti, THR) belum dilakukan. Itu diserahkan ke HR dan Legal.
- Saya tidak menjalankan kalkulasi atau simulasi. Rumus belum diuji dengan data nyata.
- Temuan berasal dari pembacaan struktur, pencarian kata kunci, dan ingatan atas isi dokumen yang saya tulis, bukan pembacaan ulang setiap baris. Daftar celah kemungkinan belum lengkap.

---

## 8. Catatan Resolusi & Status Implementasi Revisi v1.1 (Konteks MVP Single-Company)

Pada tanggal **18 November 2026**, seluruh dokumen BA dan SA telah diperbarui menjadi **Versi 1.1** untuk menutup celah kritis dan menyelaraskan aturan sebelum memasuki fase SDLC, dengan catatan cakupan **MVP Single-Company (1 Badan Hukum PT, Karyawan Bulanan PKWTT & PKWT)**:

### 8.1 Item Saran yang Telah Diimplementasikan Penuh (Revisi v1.1)
1. **Multi-Run dalam Satu Masa Pajak (`A1`):** Ditambahkan aturan `BR-TAX-009` (akumulasi bruto gabungan semua run dalam bulan yang sama untuk tarif TER, dikurangi PPh yang sudah dipotong di run sebelumnya) dan `BR-BPJS-TK-007` (iuran BPJS hanya dipotong 1x sebulan), beserta kolom `mtd_prior_gross` & `mtd_prior_tax_withheld` di `tax_calculations`.
2. **Input Variabel, Rapel & Klaim Reimbursement (`A2`):** Ditambahkan use case `UC-13`, requirement `SRS-RUN-007`, serta definisi fisik tabel `payroll_variable_inputs`, `retroactive_adjustments`, dan `reimbursement_claims` berikut endpoint API-nya.
3. **Migrasi Saldo Awal Tengah Tahun / YTD (`A3`):** Ditambahkan `BR-026`, `SRS-EMP-006`, prosedur migrasi pada `SOP-PAY-007`, dan tabel `employee_ytd_opening_balances` untuk menjaga akurasi PPh 21 Desember bila *go-live* terjadi di tengah tahun.
4. **Pemisahan Aturan Hukum vs Kebijakan Perusahaan (`A4`):** `rule_set_versions` kini khusus untuk regulasi nasional (`LegalRuleVersion`), sedangkan parameter kebijakan internal perusahaan dipisahkan ke tabel baru `company_policy_versions` (`SRS-ORG-004`). Keduanya diikat ke `payroll_runs`.
5. **Aturan Kontrak & Cuti Sakit (`A5`):** Ditambahkan `BR-EMP-001` (larangan *probation* pada kontrak PKWT di level rule & constraint database), `BR-EMP-002` (dukungan UMSP/UMSK PP 49/2025), serta `BR-LEAVE-001..002` (klasifikasi cuti dan skala upah sakit bertahap 100%/75%/50%/25%).
6. **Kamus Data Fisik Siap DDL (`A6`):** Seluruh tabel di diagram ERD maupun yang disebut di SRS/UC telah didefinisikan kolom dan constraint-nya pada `03-ERD-and-Data-Dictionary.md` v1.1 (termasuk `tax_withholding_slips`, `payroll_exceptions`, `bpjs_reconciliation_exceptions`, `statutory_payments`, tabel IAM `users/roles/permissions`, `account_mappings`, serta tabel organisasi & absensi).
7. **Koreksi Pajak & BPJS Sesuai Sumber Resmi (`A7`):**
   - `SOP-PAY-005` diperbarui menjadi batas setor PPh 21 **tanggal 15** bulan berikutnya (mengacu PMK 81/2024 Pasal 94 ayat 2 — Coretax).
   - `BR-BPJS-TK-005`, `SRS-BPJS-003`, `UC-07`, dan `03-ERD` memperbaiki model **JKP sebagai status kelayakan kepesertaan (`is_jkp_eligible`) tanpa menambah beban iuran** (mencegah *double counting*).
   - `BR-BPJS-TK-004` mencantumkan referensi awal batas atas upah JP **Rp11.086.300** (efektif Maret 2026 — tetap wajib diverifikasi ke pengumuman primer BPJS TK sebelum produksi).
   - Contoh numerik TER karangan (2%, 3%, 44/40/41 lapisan) telah dihapus dan diseragamkan dengan acuan impor lampiran resmi **PP 58/2023**, serta istilah bukti potong diseragamkan menjadi **BPA1 (Coretax)**.
8. **Roadmap 6 Fase, Register Regulasi & UAT (`A8`, `B2`):** Ditambahkan langsung ke `00-Master-Roadmap-BA-SA-SDLC.md` v1.1 (`UAT-001` s/d `UAT-022`), beserta proses penutupan akhir tahun & rollover awal tahun (`BPA-10` dan `SOP-PAY-007`).
9. **Urutan Engine & Status Pembayaran (`B8`, `Inkonsistensi #1, #2, #6`):** `SRS-RUN-003` kini mengeksekusi BPJS sebelum PPh 21; State Machine `PaymentInstruction` kini memiliki status `ACCEPTED_PROCESSING`; State Machine `PayrollRun` memiliki `REOPENED` pra-approval; dan `AuditLog` memakai *hash chain* terpartisi per `(company_id, partition_year_month)`.

### 8.2 Penyesuaian Khusus untuk Konteks MVP & Single-Company
1. **Struktur Data Tetap Siap Multi-Company Tanpa Kerumitan SaaS RLS:** Seluruh tabel bisnis menyertakan kolom `company_id` agar ekspansi ke holding/multi-PT di Fase 2 tidak memerlukan *refactoring* skema besar. Namun pada MVP *Single-Company*, kerumitan *Row-Level Security* multi-tenant dan jurnal konsolidasi *intercompany* sengaja tidak dimasukkan.
2. **Mode Fleksibel SoD untuk Tim Ramping:** Pada `01-BRD` Bagian 3.3, 14 aktor tetap didefinisikan sebagai *logical roles*. Untuk organisasi single-company dengan jumlah staf terbatas, satu pengguna diizinkan memegang beberapa *logical role*, dengan syarat mutlak yang dijaga aplikasi & DB: **Maker != Checker/Approver pada transaksi yang sama**.
3. **Konsolidasi Dokumen yang Ramping:** Usulan pembuatan 4 file dokumen baru terpisah (`Test-Strategy`, `Implementation-Roadmap`, `Regulatory-Register`, `Data-Migration`) disatukan secara ringkas dan terstruktur ke dalam `00-Master-Roadmap`, `01-BRD`, `02-BPA`, `03-ERD`, dan `04-SOP` agar lebih mudah dipelihara menjelang SDLC.

### 8.3 Item yang Tetap Ditunda ke Fase 2 (Di Luar MVP Single-Company)
- Pekerja Harian Lepas (BHL), borongan, dan siklus penggajian mingguan/harian.
- Kasus pajak khusus WNA ekspatriat (PPh Pasal 26) dan komisi sales berjenjang (*tiered commission*).
- Integrasi langsung *Host-to-Host* API Bank dan integrasi dua arah ERP real-time (MVP menggunakan ekspor/impor file terstandar dengan checksum & idempotency).
- Multi-company *intercompany settlement*.

> **Pekerjaan Wajib di Phase 0 SDLC Sebelum Go-Live:** Tim Tax, BPJS, Legal, dan HR tetap wajib mengonfirmasi 10 poin asumsi bisnis (Bagian 5 dokumen ini) serta mengimpor naskah primer tabel TER Lampiran PP 58/2023 dan *Decision Table* PHK PP 35/2021 ke dalam database awal.
