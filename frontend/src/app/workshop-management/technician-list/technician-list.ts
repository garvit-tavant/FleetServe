import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TechnicianService, TechnicianResponse } from '../services/technician';

@Component({
  selector: 'app-technician-list',
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './technician-list.html',
  styleUrl: './technician-list.scss',
})
export class TechnicianList implements OnInit {
  private technicianService = inject(TechnicianService);

  technicians = signal<TechnicianResponse[]>([]);
  loading = signal(false);
  errorMessage = signal<string | null>(null);

  displayedColumns = ['appUserId', 'workshopCode', 'hourlyRate', 'active', 'actions'];

  ngOnInit(): void {
    this.loadTechnicians();
  }

  toggleActive(technician: TechnicianResponse): void {
    const action = technician.active
      ? this.technicianService.deactivate(technician.id)
      : this.technicianService.activate(technician.id);

    action.subscribe({
      next: () => this.loadTechnicians(),
      error: (err) => this.errorMessage.set(err.error?.message || 'Action failed.'),
    });
  }

  loadTechnicians(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.technicianService.getAll().subscribe({
      next: (data) => {
        this.technicians.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load technicians.');
      },
    });
  }
}
