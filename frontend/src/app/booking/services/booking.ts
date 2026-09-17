import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface BookingResponse {
  id: number;
  assetId: number;
  workshopId: number;
  bayId: number;
  technicianId: number;
  startAt: string;
  endAt: string;
  kind: 'PREVENTIVE' | 'CORRECTIVE';
  maintenancePlanId: number | null;
  breakdownRequestId: number | null;
  status: 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';
  workOrderId: number | null;
  workOrderNumber: string | null;
  workOrderStatus: string | null;
}

export interface CancelBookingRequest {
  reason: string;
}

export interface BookingCancellationResponse {
  bookingId: number;
  bookingStatus: string;
  workOrderId: number | null;
  workOrderStatus: string | null;
  bookingKind: string;
  cancellationReason: string;
  cancelledAt: string;
}

@Injectable({ providedIn: 'root' })
export class BookingService {
  constructor(private http: HttpClient) {}

  getAll(): Observable<BookingResponse[]> {
    return this.http.get<BookingResponse[]>(`${environment.apiUrl}/bookings`);
  }

  getById(bookingId: number): Observable<BookingResponse> {
    return this.http.get<BookingResponse>(`${environment.apiUrl}/bookings/${bookingId}`);
  }

  cancel(
    bookingId: number,
    payload: CancelBookingRequest
  ): Observable<BookingCancellationResponse> {
    return this.http.post<BookingCancellationResponse>(
      `${environment.apiUrl}/bookings/${bookingId}/cancel`,
      payload
    );
  }
}
