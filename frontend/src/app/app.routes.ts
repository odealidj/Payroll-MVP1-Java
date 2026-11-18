import { Routes } from '@angular/router';
import { MasterDataComponent } from './pages/master-data/master-data.component';
import { PayrollRunComponent } from './pages/payroll-run/payroll-run.component';
import { PayslipTraceComponent } from './pages/payslip-trace/payslip-trace.component';

export const routes: Routes = [
  { path: '', redirectTo: 'payroll-run', pathMatch: 'full' },
  { path: 'payroll-run', component: PayrollRunComponent },
  { path: 'payslip-trace', component: PayslipTraceComponent },
  { path: 'payslip-trace/:runId/:resultId', component: PayslipTraceComponent },
  { path: 'master-data', component: MasterDataComponent },
  { path: '**', redirectTo: 'payroll-run' }
];
