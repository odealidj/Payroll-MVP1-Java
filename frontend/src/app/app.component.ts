import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header style="background: #0f172a; color: #fff; border-bottom: 1px solid #1e293b;">
      <div style="max-width: 1380px; margin: 0 auto; padding: 12px 24px; display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 16px;">
        <div>
          <div style="font-weight: 800; font-size: 16px; letter-spacing: 0.01em;">
            Payroll Indonesia <span style="color: #60a5fa;">Core Engine</span>
          </div>
          <div style="font-size: 11px; color: #94a3b8;">
            Modular Monolith · PP 58/2023 TER · PMK 168/2023 · BPJS 5 Program · PP 35/2021
          </div>
        </div>

        <nav style="display: flex; gap: 8px;">
          <a routerLink="/payroll-run" routerLinkActive="nav-active" class="nav-link">
            Payroll Run &amp; Simulator
          </a>
          <a routerLink="/payslip-trace" routerLinkActive="nav-active" class="nav-link">
            Slip Gaji &amp; Trace
          </a>
          <a routerLink="/master-data" routerLinkActive="nav-active" class="nav-link">
            Master Data &amp; Regulasi
          </a>
        </nav>
      </div>
    </header>

    <main class="container">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    .nav-link {
      color: #cbd5e1;
      text-decoration: none;
      font-size: 13px;
      font-weight: 600;
      padding: 7px 13px;
      border-radius: 7px;
      transition: all 0.15s ease;
    }
    .nav-link:hover {
      background: rgba(255, 255, 255, 0.08);
      color: #fff;
    }
    .nav-active {
      background: #1d4ed8 !important;
      color: #fff !important;
    }
  `]
})
export class AppComponent {}
