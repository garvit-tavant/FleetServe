import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PartStockResponse {
  partId: number;
  partNumber: string;
  partDescription: string;
  workshopId: number;
  workshopCode: string;
  onHand: number;
  reorderLevel: number;
  reorderRequired: boolean;
}

@Injectable({ providedIn: 'root' })
export class PartStockService {
  private baseUrl = `${environment.apiUrl}/v1/stock`;

  constructor(private http: HttpClient) {}

  getStock(partId: number, workshopId: number): Observable<PartStockResponse> {
    return this.http.get<PartStockResponse>(this.baseUrl, {
      params: { partId, workshopId },
    });
  }

  getForWorkshop(workshopId: number): Observable<PartStockResponse[]> {
    return this.http.get<PartStockResponse[]>(`${this.baseUrl}/workshop/${workshopId}`);
  }

  getForPart(partId: number): Observable<PartStockResponse[]> {
    return this.http.get<PartStockResponse[]>(`${this.baseUrl}/part/${partId}`);
  }

  getReorderAlerts(workshopId: number): Observable<PartStockResponse[]> {
    return this.http.get<PartStockResponse[]>(
      `${this.baseUrl}/workshop/${workshopId}/reorder-alerts`
    );
  }
}
