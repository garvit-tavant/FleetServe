import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { WorkshopService, WorkshopResponse } from '../services/workshop';
import { BayService, BayResponse } from '../services/bay';
import { TechnicianService, TechnicianResponse } from '../services/technician';

@Component({
  selector: 'app-workshop-detail',
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatTableModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './workshop-detail.html',
  styleUrl: './workshop-detail.scss',
})
export class WorkshopDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private workshopService = inject(WorkshopService);
  private bayService = inject(BayService);
  private technicianService = inject(TechnicianService);

  workshop = signal<WorkshopResponse | null>(null);
  bays = signal<BayResponse[]>([]);
  technicians = signal<TechnicianResponse[]>([]);
  loading = signal(false);
  errorMessage = signal<string | null>(null);
  technicianError = signal<string | null>(null);

  bayColumns = ['bayCode', 'active'];
  technicianColumns = ['appUserId', 'hourlyRate', 'active', 'actions'];

  private get workshopId(): number {
    return Number(this.route.snapshot.paramMap.get('id'));
  }

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.workshopService.getById(this.workshopId).subscribe({
      next: (data) => {
        this.workshop.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load workshop.');
      },
    });

    this.bayService.getByWorkshop(this.workshopId).subscribe({
      next: (data) => this.bays.set(data),
      error: () => {},
    });

    this.technicianService.getByWorkshop(this.workshopId).subscribe({
      next: (data) => {
        this.technicians.set(data);
        this.technicianError.set(null);
      },
      error: (err) => {
        this.technicianError.set(
          err.error?.message || `Failed to load technicians (status ${err.status}).`
        );
      },
    });
  }

  toggleTechnicianActive(technician: TechnicianResponse): void {
    const action = technician.active
      ? this.technicianService.deactivate(technician.id)
      : this.technicianService.activate(technician.id);

    action.subscribe({
      next: () => this.loadAll(),
      error: (err) => this.technicianError.set(err.error?.message || 'Action failed.'),
    });
  }

  toggleWorkshopActive(): void {
    const w = this.workshop();
    if (!w) return;

    const action = w.active
      ? this.workshopService.deactivate(w.id)
      : this.workshopService.activate(w.id);

    action.subscribe({
      next: () => this.loadAll(),
      error: (err) => this.errorMessage.set(err.error?.message || 'Action failed.'),
    });
  }

  addBay(): void {
    this.router.navigate(['/workshops', this.workshopId, 'bays', 'new']);
  }

  addTechnician(): void {
    this.router.navigate(['/technicians/new'], {
      queryParams: { workshopId: this.workshopId },
    });
  }
}
