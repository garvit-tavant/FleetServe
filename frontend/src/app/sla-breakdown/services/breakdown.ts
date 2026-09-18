import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type BreakdownPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type BreakdownStatus = 'REPORTED' | 'BOOKED' | 'RESOLVED' | 'CANCELLED';

export interface BreakdownRequestResponse {
  id: number;
  assetId: number;
  assetCode: string;
  depotId: number;
  priority: BreakdownPriority;
  description: string;
  status: BreakdownStatus;
  reportedById: number;
  reportedAt: string;
  slaPolicyId: number | null;
  bookingId: number | null;
}

export interface CreateBreakdownRequestRequest {
  assetId: number;
  priority: BreakdownPriority;
  description: string;
}

export interface CreateCorrectiveBookingRequest {
  requiredSkillCode: string;
  requiredCapabilityCode: string;
  estimatedDurationMinutes: number;
}

export interface CorrectiveBookingResponse {
  bookingId: number;
  assetId: number;
  workshopId: number;
  bayId: number;
  technicianId: number;
  breakdownRequestId: number;
  start: string;
  end: string;
  bookingKind: string;
  bookingStatus: string;
  workOrderId: number;
  workOrderNumber: string;
  workOrderStatus: string;
}

@Injectable({ providedIn: 'root' })
export class BreakdownService {
  private baseUrl = `${environment.apiUrl}/breakdown-requests`;

  constructor(private http: HttpClient) {}

  create(payload: CreateBreakdownRequestRequest): Observable<BreakdownRequestResponse> {
    return this.http.post<BreakdownRequestResponse>(this.baseUrl, payload);
  }

  getById(id: number): Observable<BreakdownRequestResponse> {
    return this.http.get<BreakdownRequestResponse>(`${this.baseUrl}/${id}`);
  }

  updateStatus(id: number, status: BreakdownStatus): Observable<BreakdownRequestResponse> {
    return this.http.patch<BreakdownRequestResponse>(
      `${this.baseUrl}/${id}/status`,
      {},
      { params: { status } }
    );
  }

  createCorrectiveBooking(
    breakdownId: number,
    payload: CreateCorrectiveBookingRequest
  ): Observable<CorrectiveBookingResponse> {
    return this.http.post<CorrectiveBookingResponse>(
      `${this.baseUrl}/${breakdownId}/booking`,
      payload
    );
  }
}
