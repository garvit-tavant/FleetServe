import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BreakdownPriority } from './breakdown';

export type SlaBasis = 'CALENDAR_TIME' | 'WORKING_TIME';

export interface SlaPolicyResponse {
  id: number;
  priority: BreakdownPriority;
  responseTargetMinutes: number;
  resolutionTargetMinutes: number;
  calendarBasis: SlaBasis;
  effectiveFrom: string;
  effectiveTo: string | null;
}

export interface CreateSlaPolicyRequest {
  priority: BreakdownPriority;
  responseTargetMinutes: number;
  resolutionTargetMinutes: number;
  calendarBasis: SlaBasis;
  effectiveFrom: string;
  effectiveTo?: string | null;
}

@Injectable({ providedIn: 'root' })
export class SlaPolicyService {
  private baseUrl = `${environment.apiUrl}/sla-policies`;

  constructor(private http: HttpClient) {}

  create(payload: CreateSlaPolicyRequest): Observable<SlaPolicyResponse> {
    return this.http.post<SlaPolicyResponse>(this.baseUrl, payload);
  }

  getById(id: number): Observable<SlaPolicyResponse> {
    return this.http.get<SlaPolicyResponse>(`${this.baseUrl}/${id}`);
  }

  getAll(): Observable<SlaPolicyResponse[]> {
    return this.http.get<SlaPolicyResponse[]>(this.baseUrl);
  }
}
