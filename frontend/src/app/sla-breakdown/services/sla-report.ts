import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BreakdownPriority } from './breakdown';

export interface MeanTimeToRepairReport {
  assetClassId: number;
  assetClassCode: string;
  meanTimeToRepairMinutes: number;
  sampleSize: number;
}

export interface SlaComplianceReport {
  priority: BreakdownPriority;
  evaluatedCases: number;
  compliantCases: number;
  compliancePercent: number;
}

@Injectable({ providedIn: 'root' })
export class SlaReportService {
  private baseUrl = `${environment.apiUrl}/reports`;

  constructor(private http: HttpClient) {}

  getMttr(): Observable<MeanTimeToRepairReport[]> {
    return this.http.get<MeanTimeToRepairReport[]>(`${this.baseUrl}/mttr`);
  }

  getSlaCompliance(): Observable<SlaComplianceReport[]> {
    return this.http.get<SlaComplianceReport[]>(`${this.baseUrl}/sla-compliance`);
  }
}
