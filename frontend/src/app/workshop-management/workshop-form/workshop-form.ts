import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { WorkshopService } from '../services/workshop';
import { DepotService, DepotResponse } from '../../asset-management/services/depot';

@Component({
  selector: 'app-workshop-form',
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
  templateUrl: './workshop-form.html',
  styleUrl: './workshop-form.scss',
})
export class WorkshopForm implements OnInit {
  private fb = inject(FormBuilder);
  private workshopService = inject(WorkshopService);
  private depotService = inject(DepotService);
  private router = inject(Router);

  depots = signal<DepotResponse[]>([]);
  submitting = signal(false);
  errorMessage = signal<string | null>(null);

  form = this.fb.group({
    code: ['', [Validators.required, Validators.maxLength(30)]],
    depotId: [null as number | null, Validators.required],
    timeZone: ['', [Validators.required, Validators.maxLength(100)]],
  });

  ngOnInit(): void {
    this.depotService.getAll().subscribe({
      next: (data) => this.depots.set(data),
      error: () => this.errorMessage.set('Failed to load depots.'),
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

    this.workshopService
      .create({
        code: value.code!,
        depotId: value.depotId!,
        timeZone: value.timeZone!,
      })
      .subscribe({
        next: (workshop) => {
          this.submitting.set(false);
          this.router.navigate(['/workshops', workshop.id]);
        },
        error: (err) => {
          this.submitting.set(false);
          this.errorMessage.set(err.error?.message || 'Failed to create workshop.');
        },
      });
  }
}
