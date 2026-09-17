import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BookingService, BookingResponse } from '../services/booking';

@Component({
  selector: 'app-booking-list',
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './booking-list.html',
  styleUrl: './booking-list.scss',
})
export class BookingList implements OnInit {
  private bookingService = inject(BookingService);

  bookings = signal<BookingResponse[]>([]);
  loading = signal(false);
  errorMessage = signal<string | null>(null);

  displayedColumns = ['id', 'kind', 'status', 'startAt', 'endAt', 'workOrderStatus', 'actions'];

  ngOnInit(): void {
    this.loadBookings();
  }

  loadBookings(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.bookingService.getAll().subscribe({
      next: (data) => {
        this.bookings.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to load bookings.');
      },
    });
  }
}
