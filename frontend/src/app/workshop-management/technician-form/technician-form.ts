import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TechnicianService } from '../services/technician';
import { WorkshopService, WorkshopResponse } from '../services/workshop';

@Component({
  selector: 'app-technician-form',
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './technician-form.html',
  styleUrl: './technician-form.scss',
})
export class TechnicianForm implements OnInit {
  private fb = inject(FormBuilder);
  private technicianService = inject(TechnicianService);
  private workshopService = inject(WorkshopService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  workshops = signal<WorkshopResponse[]>([]);
  submitting = signal(false);
  errorMessage = signal<string | null>(null);

  form = this.fb.group({
    appUserId: [null as number | null, [Validators.required, Validators.min(1)]],
    workshopId: [null as number | null, Validators.required],
    hourlyRate: [null as number | null, [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void {
    this.workshopService.getAll().subscribe({
      next: (data) => {
        this.workshops.set(data);

        const workshopIdParam = this.route.snapshot.queryParamMap.get('workshopId');
        if (workshopIdParam) {
          this.form.patchValue({ workshopId: Number(workshopIdParam) });
        }
      },
      error: () => this.errorMessage.set('Failed to load workshops.'),
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    this.submitting.set(true);
    this.errorMessage.set(null);

    this.technicianService
      .create({
        appUserId: value.appUserId!,
        workshopId: value.workshopId!,
        hourlyRate: value.hourlyRate!,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.router.navigate(['/technicians']);
        },
        error: (err) => {
          this.submitting.set(false);
          this.errorMessage.set(err.error?.message || 'Failed to create technician.');
        },
      });
  }
}
