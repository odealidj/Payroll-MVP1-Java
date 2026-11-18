import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApiResponse,
  Journal,
  MasterOverview,
  PayrollPeriod,
  PayrollResultDetail,
  PayrollResultSummary,
  PayrollRun,
  SimulatorOverrideRequest
} from '../models/payroll.model';

@Injectable({ providedIn: 'root' })
export class PayrollApiService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getMasterOverview(): Observable<MasterOverview> {
    return this.http.get<ApiResponse<MasterOverview>>(`${this.baseUrl}/master/overview`).pipe(map(r => r.data));
  }

  getPeriods(): Observable<PayrollPeriod[]> {
    return this.http.get<ApiResponse<PayrollPeriod[]>>(`${this.baseUrl}/payroll-periods`).pipe(map(r => r.data));
  }

  getRuns(): Observable<PayrollRun[]> {
    return this.http.get<ApiResponse<PayrollRun[]>>(`${this.baseUrl}/payroll-runs`).pipe(map(r => r.data));
  }

  calculateRun(payrollPeriodId: string, runType: string = 'REGULAR'): Observable<PayrollRun> {
    return this.http
      .post<ApiResponse<PayrollRun>>(`${this.baseUrl}/payroll-runs/calculate`, {
        payrollPeriodId,
        runType,
        lockSnapshot: true
      })
      .pipe(map(r => r.data));
  }

  approveRun(runId: string): Observable<PayrollRun> {
    return this.http.post<ApiResponse<PayrollRun>>(`${this.baseUrl}/payroll-runs/${runId}/approve`, {}).pipe(map(r => r.data));
  }

  getRunResults(runId: string): Observable<PayrollResultSummary[]> {
    return this.http.get<ApiResponse<PayrollResultSummary[]>>(`${this.baseUrl}/payroll-runs/${runId}/results`).pipe(map(r => r.data));
  }

  getResultDetail(runId: string, resultId: string): Observable<PayrollResultDetail> {
    return this.http
      .get<ApiResponse<PayrollResultDetail>>(`${this.baseUrl}/payroll-runs/${runId}/results/${resultId}`)
      .pipe(map(r => r.data));
  }

  getRunJournal(runId: string): Observable<Journal> {
    return this.http.get<ApiResponse<Journal>>(`${this.baseUrl}/payroll-runs/${runId}/journal`).pipe(map(r => r.data));
  }

  applySimulatorOverride(payload: SimulatorOverrideRequest): Observable<PayrollRun> {
    return this.http.post<ApiResponse<PayrollRun>>(`${this.baseUrl}/simulator/override`, payload).pipe(map(r => r.data));
  }

  resetSimulator(payrollPeriodId: string): Observable<PayrollRun> {
    return this.http
      .post<ApiResponse<PayrollRun>>(`${this.baseUrl}/simulator/reset?payrollPeriodId=${payrollPeriodId}`, {})
      .pipe(map(r => r.data));
  }
}
