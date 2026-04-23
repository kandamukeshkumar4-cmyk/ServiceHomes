import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { BadgeModule } from 'primeng/badge';
import { TableModule } from 'primeng/table';
import { ProgressBarModule } from 'primeng/progressbar';
import { SkeletonModule } from 'primeng/skeleton';
import { DashboardService, HostDashboardData } from './dashboard.service';

@Component({
  selector: 'app-host-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, CardModule, ButtonModule, BadgeModule, TableModule, ProgressBarModule, SkeletonModule],
  templateUrl: './host-dashboard.component.html',
  styles: [`
    .object-cover { object-fit: cover; }
  `]
})
export class HostDashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  data: HostDashboardData | null = null;

  ngOnInit(): void {
    this.dashboardService.getHostDashboard().subscribe({
      next: (d) => this.data = d,
      error: (err) => console.error('Failed to load host dashboard', err)
    });
  }

  fmtNum(n: number): string {
    return Math.round(n).toLocaleString();
  }

  fmtPct(n: number): string {
    return Math.round(n).toString();
  }
}
