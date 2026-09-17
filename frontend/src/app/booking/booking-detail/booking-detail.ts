import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BookingService, BookingResponse } from '../services/booking';

@Component({
  selector: 'app-booking-detail',
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './booking-detail.html',
  styleUrl: './booking-detail.scss',
})
export class BookingDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private bookingService = inject(BookingService);
  private fb = inject(FormBuilder);

  booking = signal<BookingResponse | null>(null);
  loading = signal(false);
  errorMessage = signal<string | null>(null);
  cancelling = signal(false);
  cancelError = signal<string | null>(null);

  cancelForm = this.fb.group({
    reason: ['', [Validators.required, Validators.maxLength(1000)]],
  });

  private get bookingId(): number {
    return Number(this.route.snapshot.paramMap.get('id'));
  }

  ngOnInit(): void {
    this.loadBooking();
  }

  loadBooking(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.bookingService.getById(this.bookingId).subscribe({
      next: (data) => {
        this.booking.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load booking.');
      },
    });
  }

  cancelBooking(): void {
    if (this.cancelForm.invalid) {
      this.cancelForm.markAllAsTouched();
      return;
    }

    this.cancelling.set(true);
    this.cancelError.set(null);

    this.bookingService
      .cancel(this.bookingId, { reason: this.cancelForm.value.reason! })
      .subscribe({
        next: () => {
          this.cancelling.set(false);
          this.loadBooking();
        },
        error: (err) => {
          this.cancelling.set(false);
          this.cancelError.set(err.error?.message || 'Failed to cancel booking.');
        },
      });
  }
}
