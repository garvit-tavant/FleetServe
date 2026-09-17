import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { WorkshopService, WorkshopResponse } from '../services/workshop';

@Component({
  selector: 'app-workshop-list',
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './workshop-list.html',
  styleUrl: './workshop-list.scss',
})
export class WorkshopList implements OnInit {
  private workshopService = inject(WorkshopService);
  private router = inject(Router);

  workshops = signal<WorkshopResponse[]>([]);
  loading = signal(false);
  errorMessage = signal<string | null>(null);

  displayedColumns = ['code', 'depotId', 'timeZone', 'active'];

  ngOnInit(): void {
    this.loadWorkshops();
  }

  loadWorkshops(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.workshopService.getAll().subscribe({
      next: (data) => {
        this.workshops.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load workshops.');
      },
    });
  }

  onRowClick(workshop: WorkshopResponse): void {
    this.router.navigate(['/workshops', workshop.id]);
  }
}
