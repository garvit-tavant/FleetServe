import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BreakdownService, BreakdownRequestResponse, BreakdownStatus, CorrectiveBookingResponse } from '../services/breakdown';

@Component({
  selector: 'app-breakdown-list',
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './breakdown-list.html',
  styleUrl: './breakdown-list.scss',
})
export class BreakdownList {
  private breakdownService = inject(BreakdownService);
  private fb = inject(FormBuilder);

  createForm = this.fb.group({
    assetId: [null as number | null, Validators.required],
    priority: ['MEDIUM', Validators.required],
    description: ['', Validators.required],
  });

  createSubmitting = signal(false);
  createError = signal<string | null>(null);
  created = signal<BreakdownRequestResponse | null>(null);

  lookupIdControl = this.fb.control<number | null>(null);
  lookupLoading = signal(false);
  lookupError = signal<string | null>(null);
  breakdown = signal<BreakdownRequestResponse | null>(null);

  statusOptions: BreakdownStatus[] = ['REPORTED', 'BOOKED', 'RESOLVED', 'CANCELLED'];
  statusPending = signal(false);
  statusError = signal<string | null>(null);

  bookingForm = this.fb.group({
    requiredSkillCode: ['', Validators.required],
    requiredCapabilityCode: ['', Validators.required],
    estimatedDurationMinutes: [null as number | null, Validators.required],
  });

  bookingSubmitting = signal(false);
  bookingError = signal<string | null>(null);
  bookingResult = signal<CorrectiveBookingResponse | null>(null);

  createCorrectiveBooking(): void {
    const current = this.breakdown();
    if (!current || this.bookingForm.invalid) {
      this.bookingForm.markAllAsTouched();
      return;
    }

    this.bookingSubmitting.set(true);
    this.bookingError.set(null);
    this.bookingResult.set(null);

    const value = this.bookingForm.getRawValue();

    this.breakdownService
      .createCorrectiveBooking(current.id, {
        requiredSkillCode: value.requiredSkillCode!,
        requiredCapabilityCode: value.requiredCapabilityCode!,
        estimatedDurationMinutes: value.estimatedDurationMinutes!,
      })
      .subscribe({
        next: (response) => {
          this.bookingSubmitting.set(false);
          this.bookingResult.set(response);
          this.lookupBreakdown();
        },
        error: (err) => {
          this.bookingSubmitting.set(false);
          this.bookingError.set(err.error?.message || 'Failed to create corrective booking.');
        },
      });
  }

  createBreakdown(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.createSubmitting.set(true);
    this.createError.set(null);
    this.created.set(null);

    const value = this.createForm.getRawValue();

    this.breakdownService
      .create({
        assetId: value.assetId!,
        priority: value.priority as any,
        description: value.description!,
      })
      .subscribe({
        next: (response) => {
          this.createSubmitting.set(false);
          this.created.set(response);
          this.breakdown.set(response);
          this.lookupIdControl.setValue(response.id);
        },
        error: (err) => {
          this.createSubmitting.set(false);
          this.createError.set(err.error?.message || 'Failed to raise breakdown.');
        },
      });
  }

  lookupBreakdown(): void {
    const id = this.lookupIdControl.value;
    if (!id) {
      return;
    }

    this.lookupLoading.set(true);
    this.lookupError.set(null);

    this.breakdownService.getById(id).subscribe({
      next: (data) => {
        this.lookupLoading.set(false);
        this.breakdown.set(data);
      },
      error: (err) => {
        this.lookupLoading.set(false);
        this.breakdown.set(null);
        this.lookupError.set(err.error?.message || 'Breakdown not found.');
      },
    });
  }

  updateStatus(status: BreakdownStatus): void {
    const current = this.breakdown();
    if (!current) {
      return;
    }

    this.statusPending.set(true);
    this.statusError.set(null);

    this.breakdownService.updateStatus(current.id, status).subscribe({
      next: (updated) => {
        this.statusPending.set(false);
        this.breakdown.set(updated);
      },
      error: (err) => {
        this.statusPending.set(false);
        this.statusError.set(err.error?.message || 'Failed to update status.');
      },
    });
  }
}
