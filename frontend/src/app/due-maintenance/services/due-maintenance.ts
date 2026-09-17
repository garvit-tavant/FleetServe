import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DueMaintenanceResponse {
  assetId: number;
  vin: string;
  maintenancePlanId: number;
  maintenancePlanCode: string;
  currentOdometerKm: number;
  nextDueKm: number;
  nextDueDate: string;
  dueStatus: string;
}

export interface PreventiveBookingResponse {
  bookingId: number;
  workOrderId: number;
  workOrderNumber: string;
  assetId: number;
  maintenancePlanId: number;
  workshopId: number;
  bayId: number;
  technicianId: number;
  start: string;
  end: string;
  bookingKind: string;
  bookingStatus: string;
  workOrderStatus: string;
}

@Injectable({ providedIn: 'root' })
export class DueMaintenanceService {
  private baseUrl = `${environment.apiUrl}/due-maintenance`;

  constructor(private http: HttpClient) {}

  getDueMaintenanceAssets(): Observable<DueMaintenanceResponse[]> {
    return this.http.get<DueMaintenanceResponse[]>(this.baseUrl);
  }

  createPreventiveBooking(
    assetId: number,
    maintenancePlanId: number
  ): Observable<PreventiveBookingResponse> {
    return this.http.post<PreventiveBookingResponse>(
      `${this.baseUrl}/assets/${assetId}/maintenance-plans/${maintenancePlanId}/bookings`,
      {}
    );
  }
}
