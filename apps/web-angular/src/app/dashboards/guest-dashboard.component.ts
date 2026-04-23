import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { BadgeModule } from 'primeng/badge';
import { SkeletonModule } from 'primeng/skeleton';
import { DashboardService, GuestDashboardData } from './dashboard.service';

@Component({
  selector: 'app-guest-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, CardModule, ButtonModule, BadgeModule, SkeletonModule],
  templateUrl: './guest-dashboard.component.html',
  styles: [`
    .object-cover { object-fit: cover; }
  `]
})
export class GuestDashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);
  data: GuestDashboardData | null = null;

  ngOnInit(): void {
    this.dashboardService.getGuestDashboard().subscribe({
      next: (d) => this.data = d,
      error: (err) => console.error('Failed to load guest dashboard', err)
    });
  }
}
