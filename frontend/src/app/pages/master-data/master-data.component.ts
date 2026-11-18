import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { MasterOverview } from '../../core/models/payroll.model';
import { PayrollApiService } from '../../core/services/payroll-api.service';
import { RupiahPipe } from '../../shared/pipes/rupiah.pipe';

@Component({
  selector: 'app-master-data',
  standalone: true,
  imports: [CommonModule, RupiahPipe],
  template: `
    <div *ngIf="overview">
      <!-- Company, Policy & Regulatory Banner -->
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(290px, 1fr)); gap: 14px; margin-bottom: 18px;">
        <div class="card">
          <div style="font-size: 11px; font-weight: 700; color: var(--primary);">BADAN HUKUM (SINGLE-COMPANY MVP)</div>
          <h3 style="margin: 4px 0;">{{ overview.company.legalName }}</h3>
          <div style="font-size: 12px; color: var(--text-muted);">
            Kode: <span class="mono">{{ overview.company.code }}</span> · NPWP: <span class="mono">{{ overview.company.npwpMasked }}</span>
          </div>
          <div style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">
            Pola Kerja: <strong>{{ overview.company.workPattern }}</strong> · Kelas Risiko JKK: <strong>{{ overview.company.jkkRiskGroup }}</strong>
          </div>
        </div>

        <div class="card">
          <div style="font-size: 11px; font-weight: 700; color: var(--success);">KEBIJAKAN PERUSAHAAN BERBARIS VERSI</div>
          <h3 style="margin: 4px 0;">{{ overview.activePolicy.versionCode }}</h3>
          <div style="font-size: 12px; color: var(--text-muted);">
            Prorata Default: <strong>{{ overview.activePolicy.defaultProrateMethod }}</strong> ({{ overview.activePolicy.workingDayDivisor }} hari)
          </div>
          <div style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">
            Batas Potongan Non-Wajib: <strong>{{ overview.activePolicy.maxNonStatutoryDedPct }}%</strong> · Ambang Variance: <strong>&plusmn;{{ overview.activePolicy.varianceWarningPct }}%</strong>
          </div>
        </div>

        <div class="card">
          <div style="font-size: 11px; font-weight: 700; color: var(--purple);">PARAMETER REGULASI NASIONAL</div>
          <h3 style="margin: 4px 0;">{{ overview.activeRuleSet.versionCode }}</h3>
          <div style="font-size: 12px; color: var(--text-muted);">{{ overview.activeRuleSet.regulationRef }}</div>
          <div style="margin-top: 6px;">
            <span class="badge badge-purple">{{ overview.terBracketCount }} Lapisan TER A/B/C PP 58/2023 Ter-Inject</span>
          </div>
        </div>
      </div>

      <!-- 10 Seeded Employee Scenarios -->
      <div class="card" style="margin-bottom: 18px;">
        <h3 class="card-title">10 Skenario Karyawan Uji (Di-Inject Otomatis via Flyway V2__seed_master_data.sql)</h3>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>NIP &amp; Nama</th>
                <th>Skenario Pengujian</th>
                <th>Kontrak &amp; Status</th>
                <th>PTKP / TER / Metode</th>
                <th>Lokasi &amp; UMP</th>
                <th>Bank &amp; Verifikasi</th>
                <th class="text-right">Upah Tetap/Bln</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let e of overview.employees">
                <td>
                  <div style="font-weight: 700;">{{ e.employeeNumber }} · {{ e.fullName }}</div>
                  <div style="font-size: 11px; color: var(--text-muted);">CC: {{ e.costCenterCode }} · Masuk: {{ e.hireDate }}</div>
                </td>
                <td>
                  <div style="font-weight: 600;">{{ e.scenarioTitle }}</div>
                  <div style="font-size: 11px; color: var(--text-muted); max-width: 340px;">{{ e.scenarioDescription }}</div>
                </td>
                <td>
                  <span class="badge badge-gray">{{ e.contractType }}</span>
                  <div *ngIf="e.terminationDate" style="font-size: 11px; color: var(--danger); margin-top: 2px;">
                    Berakhir: {{ e.terminationDate }}
                  </div>
                </td>
                <td>
                  <span class="badge badge-blue">{{ e.ptkpStatus }} (TER {{ e.terCategory }})</span>
                  <div style="margin-top: 3px;">
                    <span class="badge" [ngClass]="e.taxMethod === 'GROSS_UP' ? 'badge-purple' : (e.taxMethod === 'NET' ? 'badge-amber' : 'badge-gray')">
                      {{ e.taxMethod }}
                    </span>
                  </div>
                </td>
                <td>
                  <div>{{ e.locationName }}</div>
                  <div class="mono" style="font-size: 11px; color: var(--text-muted);">UMP: {{ e.umpAmount | rupiah }}</div>
                </td>
                <td>
                  <div class="mono">{{ e.bankCode }} {{ e.bankAccountMasked }}</div>
                  <span class="badge" [ngClass]="e.bankVerificationStatus === 'VERIFIED' ? 'badge-green' : 'badge-red'">
                    {{ e.bankVerificationStatus }}
                  </span>
                </td>
                <td class="text-right mono" style="font-weight: 700;">
                  {{ e.fixedMonthlyWage | rupiah }}
                  <div *ngIf="e.bpjsKesExtraDependents > 0" style="font-size: 10px; color: var(--amber);">
                    +{{ e.bpjsKesExtraDependents }} Keluarga Tambahan
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Regulatory Tables & Salary Components -->
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(460px, 1fr)); gap: 18px;">
        <div class="card">
          <h3 class="card-title">Konfigurasi BPJS Kesehatan &amp; TK (JKP Status-Only)</h3>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Program</th>
                  <th class="text-right">Tarif Pemberi Kerja</th>
                  <th class="text-right">Tarif Pekerja</th>
                  <th class="text-right">Batas Atas Upah</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let b of overview.bpjsConfigs">
                  <td style="font-weight: 700;">{{ b.programCode }}</td>
                  <td class="text-right mono">{{ (b.employerRate * 100).toFixed(2) }}%</td>
                  <td class="text-right mono">{{ (b.employeeRate * 100).toFixed(2) }}%</td>
                  <td class="text-right mono">{{ b.maxWageCap ? (b.maxWageCap | rupiah) : 'Tanpa Batas' }}</td>
                </tr>
                <tr style="background: var(--primary-soft);">
                  <td style="font-weight: 700;">JKP</td>
                  <td class="text-right mono">0.00% (Rekomposisi)</td>
                  <td class="text-right mono">0.00%</td>
                  <td class="text-right">Status Kepesertaan (Usia &lt; 54 thn)</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="card">
          <h3 class="card-title">Tarif Progresif Pasal 17 UU HPP (Masa Pajak Terakhir)</h3>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Lapis</th>
                  <th class="text-right">PKP Min</th>
                  <th class="text-right">PKP Max</th>
                  <th class="text-right">Tarif</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let p of overview.pasal17Brackets">
                  <td>Lapis {{ p.layerNo }}</td>
                  <td class="text-right mono">{{ p.pkpMin | rupiah }}</td>
                  <td class="text-right mono">{{ p.pkpMax ? (p.pkpMax | rupiah) : 'Tak Terbatas' }}</td>
                  <td class="text-right mono" style="font-weight: 700;">{{ (p.rate * 100).toFixed(0) }}%</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="card" style="margin-top: 18px;">
        <h3 class="card-title">Katalog Komponen Gaji Multi-Dimensi</h3>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Kode</th>
                <th>Nama Komponen</th>
                <th>Kategori</th>
                <th>Sifat</th>
                <th>Perlakuan Pajak</th>
                <th>Basis BPJS Kes</th>
                <th>Basis Lembur</th>
                <th>Akun COA</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let c of overview.salaryComponents">
                <td class="mono" style="font-weight: 600;">{{ c.code }}</td>
                <td>{{ c.name }}</td>
                <td><span class="badge badge-gray">{{ c.category }}</span></td>
                <td>{{ c.isFixed ? 'Tetap' : 'Variabel' }}</td>
                <td class="mono" style="font-size: 12px;">{{ c.taxTreatment }}</td>
                <td>{{ c.bpjsKesBase ? 'Ya' : 'Tidak' }}</td>
                <td>{{ c.overtimeBase ? 'Ya' : 'Tidak' }}</td>
                <td class="mono">{{ c.coaAccountCode }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `
})
export class MasterDataComponent implements OnInit {
  overview: MasterOverview | null = null;

  constructor(private api: PayrollApiService) {}

  ngOnInit(): void {
    this.api.getMasterOverview().subscribe(data => (this.overview = data));
  }
}
