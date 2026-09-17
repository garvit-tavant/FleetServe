import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  DueMaintenanceService,
  DueMaintenanceResponse,
  PreventiveBookingResponse,
} from '../services/due-maintenance';

@Component({
  selector: 'app-due-maintenance-list',
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './due-maintenance-list.html',
  styleUrl: './due-maintenance-list.scss',
})
export class DueMaintenanceList implements OnInit {
  private dueMaintenanceService = inject(DueMaintenanceService);

  items = signal<DueMaintenanceResponse[]>([]);
  loading = signal(false);
  errorMessage = signal<string | null>(null);

  bookingPendingAssetId = signal<number | null>(null);
  bookingError = signal<string | null>(null);
  lastBooking = signal<PreventiveBookingResponse | null>(null);

  displayedColumns = [
    'vin',
    'maintenancePlanCode',
    'currentOdometerKm',
    'nextDueKm',
    'nextDueDate',
    'dueStatus',
    'actions',
  ];

  ngOnInit(): void {
    this.loadDueMaintenance();
  }

  loadDueMaintenance(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.dueMaintenanceService.getDueMaintenanceAssets().subscribe({
      next: (data) => {
        this.items.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load due maintenance assets.');
      },
    });
  }

  bookNow(item: DueMaintenanceResponse): void {
    this.bookingPendingAssetId.set(item.assetId);
    this.bookingError.set(null);
    this.lastBooking.set(null);

    this.dueMaintenanceService
      .createPreventiveBooking(item.assetId, item.maintenancePlanId)
      .subscribe({
        next: (booking) => {
          this.bookingPendingAssetId.set(null);
          this.lastBooking.set(booking);
          this.items.update((current) =>
            current.filter((i) => i.assetId !== item.assetId)
          );
        },
        error: (err) => {
          this.bookingPendingAssetId.set(null);
          this.bookingError.set(
            err.error?.message || 'Failed to create preventive booking. No feasible slot may be available.'
          );
        },
      });
  }
}
