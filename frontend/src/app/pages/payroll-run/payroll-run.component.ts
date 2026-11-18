import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  Journal,
  PayrollPeriod,
  PayrollResultSummary,
  PayrollRun,
  SimulatorOverrideRequest
} from '../../core/models/payroll.model';
import { PayrollApiService } from '../../core/services/payroll-api.service';
import { RupiahPipe } from '../../shared/pipes/rupiah.pipe';

@Component({
  selector: 'app-payroll-run',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RupiahPipe],
  template: `
    <div *ngIf="message" class="alert" [ngClass]="'alert-' + messageType">
      {{ message }}
    </div>

    <!-- Top Control Panel -->
    <div class="card" style="margin-bottom: 18px;">
      <div style="display: flex; flex-wrap: wrap; gap: 14px; align-items: flex-end; justify-content: space-between;">
        <div style="display: flex; flex-wrap: wrap; gap: 14px; align-items: flex-end;">
          <div class="form-group" style="min-width: 260px;">
            <label>Periode Payroll</label>
            <select [(ngModel)]="selectedPeriodId" (ngModelChange)="onPeriodChange($event)">
              <option *ngFor="let p of periods" [value]="p.id">
                {{ p.periodName }} ({{ p.scheduledWorkingDays }} hr kerja)
              </option>
            </select>
          </div>

          <div class="form-group" style="min-width: 250px;" *ngIf="runsForPeriod.length > 0">
            <label>Pilih Payroll Run</label>
            <select [(ngModel)]="selectedRunId" (ngModelChange)="onRunChange($event)">
              <option *ngFor="let r of runsForPeriod" [value]="r.id">
                {{ r.runNumber }} [{{ r.runType }} - {{ r.status }}]
              </option>
            </select>
          </div>
        </div>

        <div style="display: flex; flex-wrap: wrap; gap: 8px;">
          <button class="btn btn-outline" (click)="showSimulator = !showSimulator">
            {{ showSimulator ? 'Tutup Simulator' : 'Buka Simulator Input' }}
          </button>
          <button class="btn btn-primary" [disabled]="loading || currentRun?.status === 'APPROVED'" (click)="runCalculation('REGULAR')">
            {{ loading ? 'Memproses...' : 'Hitung Ulang (REGULAR)' }}
          </button>
          <button class="btn btn-outline" [disabled]="loading" (click)="runCalculation('THR')">
            Jalankan THR Run (Multi-Run)
          </button>
          <button class="btn btn-success" [disabled]="loading || !currentRun || currentRun.status === 'APPROVED'" (click)="approveCurrentRun()">
            {{ currentRun?.status === 'APPROVED' ? 'Run Terkunci (APPROVED)' : 'Approve & Kunci Run' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Interactive Simulator Drawer -->
    <div class="card" *ngIf="showSimulator" style="margin-bottom: 18px; border-color: #93c5fd; background: #f8fbff;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
        <div>
          <h3 class="card-title" style="margin: 0;">Simulator Perubahan Input & Metode Pajak</h3>
          <small style="color: var(--text-muted);">Ubah parameter sementara untuk menguji sensitivitas Prorata, Lembur PP 35/2021, Bonus, atau Gross-Up tanpa merusak data master.</small>
        </div>
        <button class="btn btn-outline" [disabled]="loading" (click)="resetSimulation()">Reset Simulasi Periode Ini</button>
      </div>

      <div class="form-grid">
        <div class="form-group">
          <label>Karyawan Uji</label>
          <select [(ngModel)]="simForm.employmentId">
            <option *ngFor="let r of results" [value]="r.employmentId">
              {{ r.employeeNumber }} — {{ r.employeeName }} ({{ r.taxMethod }})
            </option>
          </select>
        </div>
        <div class="form-group">
          <label>Override Metode Pajak</label>
          <select [(ngModel)]="simForm.taxMethodOverride">
            <option [ngValue]="null">-- Gunakan Default Karyawan --</option>
            <option value="GROSS">GROSS (Ditanggung Karyawan)</option>
            <option value="GROSS_UP">GROSS_UP (Tunjangan Pajak)</option>
            <option value="NET">NET (Ditanggung Perusahaan)</option>
          </select>
        </div>
        <div class="form-group">
          <label>Bonus Variabel (Rp)</label>
          <input type="number" [(ngModel)]="simForm.bonusOverride" placeholder="cth: 5000000" min="0">
        </div>
        <div class="form-group">
          <label>Lembur Hari Kerja (Jam)</label>
          <input type="number" [(ngModel)]="simForm.weekdayOvertimeHoursOverride" placeholder="cth: 6" min="0" step="0.5">
        </div>
        <div class="form-group">
          <label>Lembur Hari Libur (Jam)</label>
          <input type="number" [(ngModel)]="simForm.holidayOvertimeHoursOverride" placeholder="cth: 9" min="0" step="0.5">
        </div>
        <div class="form-group">
          <label>Unpaid Leave (Hari)</label>
          <input type="number" [(ngModel)]="simForm.unpaidLeaveDaysOverride" placeholder="cth: 2" min="0" step="1">
        </div>
      </div>

      <div style="margin-top: 12px; display: flex; justify-content: flex-end;">
        <button class="btn btn-primary" [disabled]="loading || !simForm.employmentId || currentRun?.status === 'APPROVED'" (click)="applySimulation()">
          Terapkan Simulasi & Hitung Ulang
        </button>
      </div>
    </div>

    <!-- KPI Cards -->
    <div *ngIf="currentRun" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; margin-bottom: 18px;">
      <div class="card">
        <div style="font-size: 12px; color: var(--text-muted); font-weight: 600;">TOTAL GROSS EARNINGS</div>
        <div class="mono" style="font-size: 20px; font-weight: 800; margin-top: 4px;">{{ currentRun.totalGross | rupiah }}</div>
        <div style="margin-top: 6px; font-size: 12px; color: var(--text-muted);">
          {{ currentRun.totalEmployees }} Karyawan ·
          <span class="badge" [ngClass]="currentRun.status === 'APPROVED' ? 'badge-green' : 'badge-blue'">{{ currentRun.status }}</span>
        </div>
      </div>

      <div class="card">
        <div style="font-size: 12px; color: var(--text-muted); font-weight: 600;">TOTAL PPh 21 TERUTANG</div>
        <div class="mono" style="font-size: 20px; font-weight: 800; margin-top: 4px; color: var(--purple);">{{ totalPph21 | rupiah }}</div>
        <div style="margin-top: 6px; font-size: 12px; color: var(--text-muted);">
          {{ currentRun.finalTaxPeriod ? 'Mode: Rekonsiliasi Pasal 17' : 'Mode: TER PP 58/2023' }}
        </div>
      </div>

      <div class="card">
        <div style="font-size: 12px; color: var(--text-muted); font-weight: 600;">TOTAL NET PAY (TAKE-HOME)</div>
        <div class="mono" style="font-size: 20px; font-weight: 800; margin-top: 4px; color: var(--success);">{{ currentRun.totalNetPay | rupiah }}</div>
        <div style="margin-top: 6px; font-size: 12px; color: var(--text-muted);">
          Potongan Total: <span class="mono">{{ currentRun.totalDeductions | rupiah }}</span>
        </div>
      </div>

      <div class="card">
        <div style="font-size: 12px; color: var(--text-muted); font-weight: 600;">TOTAL EMPLOYER COST</div>
        <div class="mono" style="font-size: 20px; font-weight: 800; margin-top: 4px; color: var(--primary);">{{ currentRun.totalEmployerCost | rupiah }}</div>
        <div style="margin-top: 6px; font-size: 12px; color: var(--text-muted);">
          Run: <span class="mono">{{ currentRun.runNumber }}</span>
        </div>
      </div>
    </div>

    <!-- Payroll Register Table -->
    <div class="card" style="margin-bottom: 18px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
        <h3 class="card-title" style="margin: 0;">Payroll Register (Gross-to-Net)</h3>
        <span style="font-size: 12px; color: var(--text-muted);">Klik tombol <strong>Slip &amp; Trace</strong> pada baris karyawan untuk melihat rincian rumus per rupiah.</span>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>NIP &amp; Karyawan</th>
              <th>PTKP / TER</th>
              <th>Metode</th>
              <th class="text-right">Gross Earnings</th>
              <th class="text-right">BPJS Pekerja</th>
              <th class="text-right">PPh 21</th>
              <th class="text-right">Pinjaman</th>
              <th class="text-right">Net Pay</th>
              <th class="text-right">Take-Home (+Reimb)</th>
              <th>Bayar &amp; Anomali</th>
              <th>Aksi</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let r of results">
              <td>
                <div style="font-weight: 700;">{{ r.employeeNumber }} · {{ r.employeeName }}</div>
                <div style="font-size: 11px; color: var(--text-muted);">{{ r.scenarioTitle }}</div>
              </td>
              <td>
                <span class="badge badge-gray">{{ r.ptkpStatus }} · TER {{ r.terCategory }}</span>
                <div style="font-size: 11px; color: var(--text-muted); margin-top: 2px;">{{ r.contractType }}</div>
              </td>
              <td>
                <span class="badge"
                      [ngClass]="r.taxMethod === 'GROSS_UP' ? 'badge-purple' : (r.taxMethod === 'NET' ? 'badge-amber' : 'badge-blue')">
                  {{ r.taxMethod }}
                </span>
                <div *ngIf="r.taxAllowance > 0" style="font-size: 11px; color: var(--purple); margin-top: 2px;">
                  +Tunj.Pjk {{ r.taxAllowance | rupiah }}
                </div>
              </td>
              <td class="text-right mono">{{ r.grossEarnings | rupiah }}</td>
              <td class="text-right mono">{{ r.bpjsEmployeeTotal | rupiah }}</td>
              <td class="text-right mono" [style.color]="r.pph21Amount < 0 ? 'var(--success)' : 'inherit'" [style.fontWeight]="r.pph21Amount < 0 ? '700' : '400'">
                {{ r.pph21Amount | rupiah }}
                <div *ngIf="r.pph21Amount < 0" style="font-size: 10px; color: var(--success);">TAX REFUND</div>
              </td>
              <td class="text-right mono">{{ r.loanDeduction | rupiah }}</td>
              <td class="text-right mono" style="font-weight: 700;">{{ r.netPay | rupiah }}</td>
              <td class="text-right mono" style="font-weight: 700; color: var(--success);">
                {{ r.totalTakeHomeTransfer | rupiah }}
                <div *ngIf="r.totalReimbursements > 0" style="font-size: 10px; color: var(--text-muted);">
                  (Incl. Reimb {{ r.totalReimbursements | rupiah }})
                </div>
              </td>
              <td>
                <span class="badge" [ngClass]="r.paymentStatus === 'ON_HOLD' ? 'badge-red' : 'badge-green'">
                  {{ r.paymentStatus }}
                </span>
                <div *ngFor="let flag of r.anomalyFlags" style="margin-top: 3px;">
                  <span class="badge" [ngClass]="flag.includes('REFUND') ? 'badge-green' : (flag.includes('SIMULATOR') ? 'badge-blue' : 'badge-amber')">
                    {{ flag }}
                  </span>
                </div>
              </td>
              <td>
                <a class="btn btn-outline" style="padding: 5px 10px; font-size: 12px; text-decoration: none;"
                   [routerLink]="['/payslip-trace', r.payrollRunId, r.id]">
                  Slip &amp; Trace
                </a>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Balanced Accounting Journal -->
    <div class="card" *ngIf="journal">
      <div style="display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 10px; margin-bottom: 12px;">
        <div>
          <h3 class="card-title" style="margin: 0;">Jurnal Akuntansi Otomatis ({{ journal.journalNumber }})</h3>
          <small style="color: var(--text-muted);">Posting Date: {{ journal.postingDate }} · Status: {{ journal.status }}</small>
        </div>
        <div style="display: flex; gap: 10px; align-items: center;">
          <span class="badge" [ngClass]="journal.balanced ? 'badge-green' : 'badge-red'">
            {{ journal.balanced ? 'BALANCED (Debit == Kredit)' : 'UNBALANCED' }}
          </span>
          <span class="mono" style="font-weight: 700;">Debit: {{ journal.totalDebit | rupiah }} | Kredit: {{ journal.totalCredit | rupiah }}</span>
        </div>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>Cost Center</th>
              <th>Kode COA</th>
              <th>Nama Akun</th>
              <th>Keterangan</th>
              <th class="text-right">Debit</th>
              <th class="text-right">Kredit</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let l of journal.lines">
              <td>{{ l.lineNo }}</td>
              <td><span class="badge badge-gray">{{ l.costCenterCode }}</span></td>
              <td class="mono">{{ l.coaAccountCode }}</td>
              <td style="font-weight: 600;">{{ l.accountName }}</td>
              <td style="color: var(--text-muted); font-size: 12px;">{{ l.description }}</td>
              <td class="text-right mono">{{ l.debit > 0 ? (l.debit | rupiah) : '-' }}</td>
              <td class="text-right mono">{{ l.credit > 0 ? (l.credit | rupiah) : '-' }}</td>
            </tr>
          </tbody>
          <tfoot>
            <tr style="background: var(--surface-alt); font-weight: 800;">
              <td colspan="5" class="text-right">TOTAL JURNAL</td>
              <td class="text-right mono">{{ journal.totalDebit | rupiah }}</td>
              <td class="text-right mono">{{ journal.totalCredit | rupiah }}</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  `
})
export class PayrollRunComponent implements OnInit {
  periods: PayrollPeriod[] = [];
  allRuns: PayrollRun[] = [];
  runsForPeriod: PayrollRun[] = [];
  results: PayrollResultSummary[] = [];
  journal: Journal | null = null;

  selectedPeriodId = '';
  selectedRunId = '';
  currentRun: PayrollRun | null = null;

  showSimulator = false;
  loading = false;
  message = '';
  messageType: 'info' | 'success' | 'error' = 'info';

  simForm: SimulatorOverrideRequest = {
    payrollPeriodId: '',
    employmentId: '',
    taxMethodOverride: null,
    bonusOverride: null,
    weekdayOvertimeHoursOverride: null,
    holidayOvertimeHoursOverride: null,
    unpaidLeaveDaysOverride: null
  };

  constructor(private api: PayrollApiService) {}

  ngOnInit(): void {
    this.loadInitialData();
  }

  get totalPph21(): number {
    return this.results.reduce((acc, r) => acc + (r.pph21Amount || 0), 0);
  }

  loadInitialData(): void {
    this.loading = true;
    this.api.getPeriods().subscribe({
      next: periods => {
        this.periods = periods;
        if (periods.length > 0 && !this.selectedPeriodId) {
          this.selectedPeriodId = periods[0].id;
        }
        this.loadRuns();
      },
      error: () => {
        this.showMessage('Gagal memuat daftar periode payroll dari backend.', 'error');
        this.loading = false;
      }
    });
  }

  loadRuns(preferredRunId?: string): void {
    this.api.getRuns().subscribe({
      next: runs => {
        this.allRuns = runs;
        this.filterRunsForPeriod(preferredRunId);
        this.loading = false;
      },
      error: () => {
        this.showMessage('Gagal memuat daftar payroll run.', 'error');
        this.loading = false;
      }
    });
  }

  onPeriodChange(periodId: string): void {
    this.selectedPeriodId = periodId;
    this.filterRunsForPeriod();
  }

  filterRunsForPeriod(preferredRunId?: string): void {
    this.runsForPeriod = this.allRuns.filter(r => r.payrollPeriodId === this.selectedPeriodId);
    if (this.runsForPeriod.length > 0) {
      const target = preferredRunId
        ? this.runsForPeriod.find(r => r.id === preferredRunId) || this.runsForPeriod[0]
        : this.runsForPeriod[0];
      this.selectedRunId = target.id;
      this.currentRun = target;
      this.loadRunDetails(target.id);
    } else {
      this.selectedRunId = '';
      this.currentRun = null;
      this.results = [];
      this.journal = null;
    }
  }

  onRunChange(runId: string): void {
    this.selectedRunId = runId;
    this.currentRun = this.runsForPeriod.find(r => r.id === runId) || null;
    if (this.currentRun) {
      this.loadRunDetails(this.currentRun.id);
    }
  }

  loadRunDetails(runId: string): void {
    this.api.getRunResults(runId).subscribe(res => {
      this.results = res;
      if (res.length > 0 && !this.simForm.employmentId) {
        this.simForm.employmentId = res[0].employmentId;
      }
      this.simForm.payrollPeriodId = this.selectedPeriodId;
    });
    this.api.getRunJournal(runId).subscribe({
      next: j => (this.journal = j),
      error: () => (this.journal = null)
    });
  }

  runCalculation(runType: string): void {
    if (!this.selectedPeriodId) return;
    this.loading = true;
    this.api.calculateRun(this.selectedPeriodId, runType).subscribe({
      next: run => {
        this.showMessage(`Berhasil menjalankan kalkulasi ${run.runType} (${run.runNumber}) untuk ${run.totalEmployees} karyawan.`, 'success');
        this.loadRuns(run.id);
      },
      error: err => {
        const msg = err?.error?.error?.message || 'Gagal menjalankan kalkulasi payroll.';
        this.showMessage(msg, 'error');
        this.loading = false;
      }
    });
  }

  approveCurrentRun(): void {
    if (!this.currentRun) return;
    this.loading = true;
    this.api.approveRun(this.currentRun.id).subscribe({
      next: run => {
        this.showMessage(`Run ${run.runNumber} resmi disetujui (APPROVED & Immutable).`, 'success');
        this.loadRuns(run.id);
      },
      error: err => {
        const msg = err?.error?.error?.message || 'Gagal menyetujui run.';
        this.showMessage(msg, 'error');
        this.loading = false;
      }
    });
  }

  applySimulation(): void {
    this.loading = true;
    this.simForm.payrollPeriodId = this.selectedPeriodId;
    this.api.applySimulatorOverride(this.simForm).subscribe({
      next: run => {
        this.showMessage('Simulasi diterapkan dan payroll berhasil dihitung ulang.', 'success');
        this.loadRuns(run.id);
      },
      error: err => {
        const msg = err?.error?.error?.message || 'Gagal menerapkan simulasi.';
        this.showMessage(msg, 'error');
        this.loading = false;
      }
    });
  }

  resetSimulation(): void {
    if (!this.selectedPeriodId) return;
    this.loading = true;
    this.api.resetSimulator(this.selectedPeriodId).subscribe({
      next: run => {
        this.simForm.taxMethodOverride = null;
        this.simForm.bonusOverride = null;
        this.simForm.weekdayOvertimeHoursOverride = null;
        this.simForm.holidayOvertimeHoursOverride = null;
        this.simForm.unpaidLeaveDaysOverride = null;
        this.showMessage('Simulasi direset ke data seed awal dan dihitung ulang.', 'info');
        this.loadRuns(run.id);
      },
      error: () => {
        this.showMessage('Gagal mereset simulasi.', 'error');
        this.loading = false;
      }
    });
  }

  private showMessage(msg: string, type: 'info' | 'success' | 'error'): void {
    this.message = msg;
    this.messageType = type;
  }
}
