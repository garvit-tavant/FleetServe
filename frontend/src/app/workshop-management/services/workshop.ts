import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface WorkshopResponse {
  id: number;
  code: string;
  depotId: number;
  timeZone: string;
  active: boolean;
}

export interface CreateWorkshopRequest {
  code: string;
  depotId: number;
  timeZone: string;
}

@Injectable({ providedIn: 'root' })
export class WorkshopService {
  constructor(private http: HttpClient) {}

  getAll(): Observable<WorkshopResponse[]> {
    return this.http.get<WorkshopResponse[]>(`${environment.apiUrl}/workshops`);
  }

  getById(workshopId: number): Observable<WorkshopResponse> {
    return this.http.get<WorkshopResponse>(`${environment.apiUrl}/workshops/${workshopId}`);
  }

  create(payload: CreateWorkshopRequest): Observable<WorkshopResponse> {
    return this.http.post<WorkshopResponse>(`${environment.apiUrl}/workshops`, payload);
  }

  activate(workshopId: number): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/workshops/${workshopId}/activate`, {});
  }

  deactivate(workshopId: number): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/workshops/${workshopId}/deactivate`, {});
  }
}
