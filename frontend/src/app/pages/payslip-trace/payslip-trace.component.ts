import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  PayrollDetailItem,
  PayrollResultDetail,
  PayrollResultSummary,
  PayrollRun
} from '../../core/models/payroll.model';
import { PayrollApiService } from '../../core/services/payroll-api.service';
import { RupiahPipe } from '../../shared/pipes/rupiah.pipe';

@Component({
  selector: 'app-payslip-trace',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RupiahPipe],
  template: `
    <div class="card" style="margin-bottom: 18px;">
      <div style="display: flex; flex-wrap: wrap; gap: 14px; align-items: flex-end; justify-content: space-between;">
        <div style="display: flex; flex-wrap: wrap; gap: 14px; align-items: flex-end;">
          <div class="form-group" style="min-width: 260px;">
            <label>Pilih Payroll Run</label>
            <select [(ngModel)]="selectedRunId" (ngModelChange)="onRunChange($event)">
              <option *ngFor="let r of runs" [value]="r.id">
                {{ r.periodName }} — {{ r.runNumber }} ({{ r.runType }})
              </option>
            </select>
          </div>

          <div class="form-group" style="min-width: 280px;">
            <label>Pilih Karyawan</label>
            <select [(ngModel)]="selectedResultId" (ngModelChange)="onResultChange($event)">
              <option *ngFor="let res of runResults" [value]="res.id">
                {{ res.employeeNumber }} — {{ res.employeeName }} ({{ res.taxMethod }})
              </option>
            </select>
          </div>
        </div>

        <a class="btn btn-outline" routerLink="/payroll-run" style="text-decoration: none;">
          &larr; Kembali ke Payroll Register
        </a>
      </div>
    </div>

    <div *ngIf="detail" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(460px, 1fr)); gap: 18px; align-items: start;">
      <!-- LEFT COLUMN: SLIP GAJI RESMI -->
      <div class="card">
        <div style="display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 2px solid var(--border); padding-bottom: 12px; margin-bottom: 14px;">
          <div>
            <div style="font-size: 11px; font-weight: 700; color: var(--primary); letter-spacing: 0.05em;">SLIP GAJI KARYAWAN (PAYSLIP)</div>
            <h2 style="margin: 4px 0 2px; font-size: 19px;">{{ detail.summary.employeeName }}</h2>
            <div style="font-size: 12px; color: var(--text-muted);">
              {{ detail.summary.employeeNumber }} · {{ detail.summary.contractType }} · {{ detail.summary.ptkpStatus }} (TER {{ detail.summary.terCategory }})
            </div>
            <div style="font-size: 12px; color: var(--text-muted); margin-top: 2px;">
              {{ detail.summary.scenarioTitle }}
            </div>
          </div>
          <div style="text-align: right;">
            <span class="badge" [ngClass]="detail.summary.taxMethod === 'GROSS_UP' ? 'badge-purple' : (detail.summary.taxMethod === 'NET' ? 'badge-amber' : 'badge-blue')">
              {{ detail.summary.taxMethod }}
            </span>
            <div style="margin-top: 6px;">
              <span class="badge" [ngClass]="detail.summary.paymentStatus === 'ON_HOLD' ? 'badge-red' : 'badge-green'">
                {{ detail.summary.paymentStatus }}
              </span>
            </div>
          </div>
        </div>

        <!-- EARNINGS -->
        <h4 style="margin: 0 0 8px; font-size: 12px; color: var(--success); text-transform: uppercase;">A. Penghasilan (Earnings)</h4>
        <div class="table-wrap" style="margin-bottom: 14px;">
          <table>
            <tbody>
              <tr *ngFor="let item of itemsByCategory('EARNING')">
                <td>
                  <div style="font-weight: 600;">{{ item.componentName }}</div>
                  <div style="font-size: 11px; color: var(--text-muted);">{{ item.formulaDesc }}</div>
                </td>
                <td class="text-right mono" style="font-weight: 600; white-space: nowrap;">{{ item.amount | rupiah }}</td>
              </tr>
              <tr style="background: var(--success-soft); font-weight: 700;">
                <td>Total Gross Earnings</td>
                <td class="text-right mono">{{ detail.summary.grossEarnings | rupiah }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- REIMBURSEMENTS (IF ANY) -->
        <ng-container *ngIf="itemsByCategory('REIMBURSEMENT').length > 0">
          <h4 style="margin: 0 0 8px; font-size: 12px; color: var(--primary); text-transform: uppercase;">B. Reimbursement (Non-Upah / Non-Pajak)</h4>
          <div class="table-wrap" style="margin-bottom: 14px;">
            <table>
              <tbody>
                <tr *ngFor="let item of itemsByCategory('REIMBURSEMENT')">
                  <td>
                    <div style="font-weight: 600;">{{ item.componentName }}</div>
                    <div style="font-size: 11px; color: var(--text-muted);">{{ item.formulaDesc }}</div>
                  </td>
                  <td class="text-right mono" style="font-weight: 600;">{{ item.amount | rupiah }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </ng-container>

        <!-- DEDUCTIONS -->
        <h4 style="margin: 0 0 8px; font-size: 12px; color: var(--danger); text-transform: uppercase;">C. Potongan (Deductions)</h4>
        <div class="table-wrap" style="margin-bottom: 14px;">
          <table>
            <tbody>
              <tr *ngFor="let item of itemsByCategory('DEDUCTION')">
                <td>
                  <div style="font-weight: 600;">{{ item.componentName }}</div>
                  <div style="font-size: 11px; color: var(--text-muted);">{{ item.formulaDesc }}</div>
                </td>
                <td class="text-right mono" [style.color]="item.amount < 0 ? 'var(--success)' : 'var(--danger)'" style="font-weight: 600; white-space: nowrap;">
                  {{ item.amount | rupiah }}
                </td>
              </tr>
              <tr style="background: var(--danger-soft); font-weight: 700;">
                <td>Total Potongan</td>
                <td class="text-right mono">{{ detail.summary.totalDeductions | rupiah }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- NET PAY & TAKE HOME SUMMARY -->
        <div style="background: #0f172a; color: #fff; border-radius: 8px; padding: 14px 16px; margin-bottom: 14px;">
          <div style="display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 13px; color: #cbd5e1;">
            <span>Net Pay (Gross - Potongan)</span>
            <span class="mono">{{ detail.summary.netPay | rupiah }}</span>
          </div>
          <div style="display: flex; justify-content: space-between; font-size: 17px; font-weight: 800;">
            <span>Total Take-Home Transfer</span>
            <span class="mono" style="color: #34d399;">{{ detail.summary.totalTakeHomeTransfer | rupiah }}</span>
          </div>
        </div>

        <!-- EMPLOYER CONTRIBUTIONS -->
        <h4 style="margin: 0 0 8px; font-size: 12px; color: var(--text-muted); text-transform: uppercase;">D. Iuran &amp; Beban Perusahaan (Employer Cost)</h4>
        <div class="table-wrap">
          <table>
            <tbody>
              <tr *ngFor="let item of employerAndMemoItems()">
                <td>
                  <div style="font-weight: 600;">{{ item.componentName }}</div>
                  <div style="font-size: 11px; color: var(--text-muted);">{{ item.formulaDesc }}</div>
                </td>
                <td class="text-right mono" style="white-space: nowrap;">{{ item.amount | rupiah }}</td>
              </tr>
              <tr style="background: var(--surface-alt); font-weight: 700;">
                <td>Total Biaya Perusahaan (Total Employer Cost)</td>
                <td class="text-right mono">{{ detail.summary.totalEmployerCost | rupiah }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- RIGHT COLUMN: EXPLAINABLE CALCULATION TRACE -->
      <div class="card">
        <div style="border-bottom: 2px solid var(--border); padding-bottom: 12px; margin-bottom: 14px;">
          <div style="font-size: 11px; font-weight: 700; color: var(--purple); letter-spacing: 0.05em;">EXPLAINABLE CALCULATION TRACE</div>
          <h3 style="margin: 4px 0 2px; font-size: 17px;">Jejak Rumus Deterministik &amp; Parameter</h3>
          <div style="font-size: 12px; color: var(--text-muted);">
            Engine v{{ traceStr('engineVersion') }} · Legal Rule: <strong>{{ traceStr('legalRuleVersionCode') }}</strong> · Policy: <strong>{{ traceStr('companyPolicyVersionCode') }}</strong>
          </div>
        </div>

        <!-- Prorata Trace -->
        <div style="background: var(--surface-alt); border: 1px solid var(--border); border-radius: 8px; padding: 12px; margin-bottom: 12px;">
          <div style="font-weight: 700; margin-bottom: 6px;">1. Prorata &amp; Multi-Segment Effective Dating</div>
          <div *ngIf="prorataSegments.length > 0; else noSegments">
            <div *ngFor="let seg of prorataSegments" style="font-size: 12px; padding: 4px 0; border-bottom: 1px dashed var(--border);">
              <strong>{{ seg['component'] }}</strong> ({{ seg['from'] }} s/d {{ seg['to'] }}):
              <span class="mono">{{ seg['paidDays'] }}/{{ seg['totalPeriodDays'] }}</span> hari
              (faktor <span class="mono">{{ seg['factor'] }}</span>) &times;
              <span class="mono">{{ seg['monthlyAmount'] | rupiah }}</span> =
              <span class="mono" style="font-weight: 700;">{{ seg['exactAmount'] | rupiah }}</span>
            </div>
          </div>
          <ng-template #noSegments>
            <div style="font-size: 12px; color: var(--text-muted);">Run tipe {{ traceStr('runType') }} (tanpa prorata harian).</div>
          </ng-template>
        </div>

        <!-- Overtime Trace -->
        <div *ngIf="overtimeTrace" style="background: var(--surface-alt); border: 1px solid var(--border); border-radius: 8px; padding: 12px; margin-bottom: 12px;">
          <div style="font-weight: 700; margin-bottom: 6px;">2. Lembur PP 35/2021 (Basis / 173)</div>
          <div style="font-size: 12px; margin-bottom: 6px;">
            Upah Dasar: <span class="mono">{{ overtimeTrace['baseWageUsed'] | rupiah }}</span> &rarr;
            Upah Sejam (<span class="mono">1/173</span>): <strong class="mono">{{ overtimeTrace['hourlyRate173'] | rupiah }}</strong>
          </div>
          <div *ngFor="let b of overtimeBreakdown" style="font-size: 12px; display: flex; justify-content: space-between; padding: 2px 0;">
            <span>{{ b['dayCategory'] }} — {{ b['label'] }} ({{ b['hours'] }} jam &times; {{ b['multiplier'] }}x)</span>
            <span class="mono">{{ b['amount'] | rupiah }}</span>
          </div>
        </div>

        <!-- BPJS Trace -->
        <div *ngIf="bpjsTrace" style="background: var(--surface-alt); border: 1px solid var(--border); border-radius: 8px; padding: 12px; margin-bottom: 12px;">
          <div style="font-weight: 700; margin-bottom: 6px;">3. BPJS Kesehatan &amp; 5 Program BPJS TK</div>
          <div style="font-size: 12px; line-height: 1.7;">
            <div>
              <strong>BPJS Kesehatan:</strong> Basis Terbatas <span class="mono">{{ bpjsKes['cappedBase'] | rupiah }}</span>
              (Er 4%: <span class="mono">{{ bpjsKes['employer4Pct'] | rupiah }}</span>, Emp 1%: <span class="mono">{{ bpjsKes['employee1Pct'] | rupiah }}</span>
              <span *ngIf="num(bpjsKes['extraDependentsAmount']) > 0">, Tambahan Keluarga: <span class="mono">{{ bpjsKes['extraDependentsAmount'] | rupiah }}</span></span>)
            </div>
            <div>
              <strong>JHT:</strong> Er 3.7% <span class="mono">{{ bpjsTk['JHT']['employer37Pct'] | rupiah }}</span> · Emp 2% <span class="mono">{{ bpjsTk['JHT']['employee20Pct'] | rupiah }}</span>
            </div>
            <div>
              <strong>JKK &amp; JKM (Er):</strong> JKK 0.24% <span class="mono">{{ bpjsTk['JKK']['employer024Pct'] | rupiah }}</span> · JKM 0.30% <span class="mono">{{ bpjsTk['JKM']['employer030Pct'] | rupiah }}</span>
            </div>
            <div>
              <strong>JP (Cap {{ bpjsTk['JP']['capLimit'] | rupiah }}):</strong>
              Basis <span class="mono">{{ bpjsTk['JP']['cappedBase'] | rupiah }}</span> ·
              Er 2% <span class="mono">{{ bpjsTk['JP']['employer20Pct'] | rupiah }}</span> ·
              Emp 1% <span class="mono">{{ bpjsTk['JP']['employee10Pct'] | rupiah }}</span>
            </div>
            <div>
              <strong>JKP:</strong>
              <span class="badge badge-blue">{{ bpjsTk['JKP']['isEligible'] ? 'ELIGIBLE (0 Iuran Tambahan)' : 'NOT ELIGIBLE' }}</span>
              — <small style="color: var(--text-muted);">{{ bpjsTk['JKP']['note'] }}</small>
            </div>
          </div>
        </div>

        <!-- PPh 21 Trace -->
        <div *ngIf="pphTrace" style="background: var(--purple-soft); border: 1px solid #ddd6fe; border-radius: 8px; padding: 12px; margin-bottom: 14px;">
          <div style="font-weight: 700; margin-bottom: 6px; color: var(--purple);">
            4. PPh Pasal 21 ({{ pphTrace['mode'] }})
          </div>
          <div style="font-size: 12px; line-height: 1.7;">
            <div>Status: <strong>{{ pphTrace['ptkpStatus'] }}</strong> · TER <strong>{{ pphTrace['terCategory'] }}</strong> · Metode: <strong>{{ pphTrace['taxMethod'] }}</strong></div>
            <div>Bruto Pajak Run Ini: <strong class="mono">{{ pphTrace['currentRunGrossTaxable'] | rupiah }}</strong></div>

            <ng-container *ngIf="pphTrace['terRateApplied'] !== undefined">
              <div>MTD Prior Gross: <span class="mono">{{ pphTrace['mtdPriorGross'] | rupiah }}</span> · Total MTD Gross: <span class="mono">{{ pphTrace['mtdTotalGrossTaxable'] | rupiah }}</span></div>
              <div>Tarif TER Diterapkan: <strong class="mono">{{ (num(pphTrace['terRateApplied']) * 100).toFixed(2) }}%</strong></div>
            </ng-container>

            <ng-container *ngIf="pphTrace['iterations'] !== undefined">
              <div>Konvergensi Solver Gross-Up: <strong>{{ pphTrace['iterations'] }} iterasi</strong> · Tunjangan Pajak: <strong class="mono">{{ pphTrace['taxAllowance'] | rupiah }}</strong></div>
            </ng-container>

            <ng-container *ngIf="pphTrace['mode'] === 'FINAL_PERIOD_PASAL_17'">
              <div>Bruto Setahun ({{ pphTrace['activeMonthsInYear'] }} bln): <span class="mono">{{ pphTrace['annualGrossTaxable'] | rupiah }}</span></div>
              <div>Biaya Jabatan: <span class="mono">{{ pphTrace['biayaJabatan'] | rupiah }}</span> · Iuran JHT+JP Pekerja Setahun: <span class="mono">{{ pphTrace['annualJhtJpWorker'] | rupiah }}</span></div>
              <div>Neto Setahun: <span class="mono">{{ pphTrace['annualNeto'] | rupiah }}</span> · PTKP: <span class="mono">{{ pphTrace['ptkpAnnual'] | rupiah }}</span> · PKP (Floor Ribuan): <strong class="mono">{{ pphTrace['pkpAnnual'] | rupiah }}</strong></div>
              <div>PPh Setahun Pasal 17: <strong class="mono">{{ pphTrace['annualTaxDue'] | rupiah }}</strong> · Sudah Dipotong Jan–Masa Lalu: <span class="mono">{{ pphTrace['previouslyWithheld'] | rupiah }}</span></div>
            </ng-container>

            <div style="margin-top: 4px; font-size: 13px;">
              <strong>PPh 21 Run Ini: </strong>
              <span class="mono" [style.color]="num(pphTrace['pph21Amount']) < 0 ? 'var(--success)' : 'var(--purple)'" style="font-weight: 800;">
                {{ pphTrace['pph21Amount'] | rupiah }}
              </span>
              <span *ngIf="pphTrace['isRefund']" class="badge badge-green" style="margin-left: 6px;">LEBIH POTONG — DIKEMBALIKAN KE KARYAWAN</span>
            </div>
          </div>
        </div>

        <!-- Raw JSON Inspector -->
        <div>
          <button class="btn btn-outline" style="font-size: 12px; padding: 5px 10px;" (click)="showRawJson = !showRawJson">
            {{ showRawJson ? 'Sembunyikan Raw calculation_trace_json' : 'Lihat Raw calculation_trace_json' }}
          </button>
          <pre *ngIf="showRawJson" class="mono" style="margin-top: 10px; background: #0f172a; color: #e2e8f0; padding: 12px; border-radius: 8px; font-size: 11px; overflow-x: auto; max-height: 360px;">{{ detail.calculationTrace | json }}</pre>
        </div>
      </div>
    </div>
  `
})
export class PayslipTraceComponent implements OnInit {
  runs: PayrollRun[] = [];
  runResults: PayrollResultSummary[] = [];
  detail: PayrollResultDetail | null = null;

  selectedRunId = '';
  selectedResultId = '';
  showRawJson = false;

  constructor(
    private api: PayrollApiService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const paramRunId = this.route.snapshot.paramMap.get('runId');
    const paramResultId = this.route.snapshot.paramMap.get('resultId');

    this.api.getRuns().subscribe(runs => {
      this.runs = runs;
      if (runs.length === 0) return;
      this.selectedRunId = paramRunId || runs[0].id;
      this.loadResultsForRun(this.selectedRunId, paramResultId || undefined);
    });
  }

  onRunChange(runId: string): void {
    this.selectedRunId = runId;
    this.loadResultsForRun(runId);
  }

  onResultChange(resultId: string): void {
    this.selectedResultId = resultId;
    this.loadDetail(this.selectedRunId, resultId);
    this.router.navigate(['/payslip-trace', this.selectedRunId, resultId], { replaceUrl: true });
  }

  private loadResultsForRun(runId: string, preferredResultId?: string): void {
    this.api.getRunResults(runId).subscribe(res => {
      this.runResults = res;
      if (res.length === 0) {
        this.detail = null;
        return;
      }
      const target = preferredResultId ? res.find(x => x.id === preferredResultId) || res[0] : res[0];
      this.selectedResultId = target.id;
      this.loadDetail(runId, target.id);
    });
  }

  private loadDetail(runId: string, resultId: string): void {
    this.api.getResultDetail(runId, resultId).subscribe(d => (this.detail = d));
  }

  itemsByCategory(cat: string): PayrollDetailItem[] {
    return this.detail ? this.detail.items.filter(i => i.category === cat) : [];
  }

  employerAndMemoItems(): PayrollDetailItem[] {
    return this.detail ? this.detail.items.filter(i => i.category === 'EMPLOYER_CONTRIBUTION' || i.category === 'MEMO') : [];
  }

  traceStr(key: string): string {
    if (!this.detail) return '-';
    return String(this.detail.calculationTrace[key] ?? '-');
  }

  get prorataSegments(): Array<Record<string, any>> {
    const p = this.detail?.calculationTrace['proration'] as Record<string, any> | undefined;
    return p && Array.isArray(p['segments']) ? p['segments'] : [];
  }

  get overtimeTrace(): Record<string, any> | null {
    return (this.detail?.calculationTrace['overtime'] as Record<string, any>) || null;
  }

  get overtimeBreakdown(): Array<Record<string, any>> {
    return this.overtimeTrace && Array.isArray(this.overtimeTrace['breakdown']) ? this.overtimeTrace['breakdown'] : [];
  }

  get bpjsTrace(): Record<string, any> | null {
    return (this.detail?.calculationTrace['bpjs'] as Record<string, any>) || null;
  }

  get bpjsKes(): Record<string, any> {
    return (this.bpjsTrace?.['kesehatan'] as Record<string, any>) || {};
  }

  get bpjsTk(): Record<string, any> {
    return (this.bpjsTrace?.['ketenagakerjaan'] as Record<string, any>) || {};
  }

  get pphTrace(): Record<string, any> | null {
    return (this.detail?.calculationTrace['pph21'] as Record<string, any>) || null;
  }

  num(val: unknown): number {
    return Number(val ?? 0);
  }
}
