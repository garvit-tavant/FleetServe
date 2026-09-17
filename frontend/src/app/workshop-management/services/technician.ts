import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface TechnicianResponse {
  id: number;
  appUserId: number;
  workshopId: number;
  workshopCode: string;
  hourlyRate: number;
  active: boolean;
}

export interface CreateTechnicianRequest {
  appUserId: number;
  workshopId: number;
  hourlyRate: number;
}

@Injectable({ providedIn: 'root' })
export class TechnicianService {
  constructor(private http: HttpClient) {}

  getAll(): Observable<TechnicianResponse[]> {
    return this.http.get<TechnicianResponse[]>(`${environment.apiUrl}/technicians`);
  }

  getById(technicianId: number): Observable<TechnicianResponse> {
    return this.http.get<TechnicianResponse>(`${environment.apiUrl}/technicians/${technicianId}`);
  }

  getByWorkshop(workshopId: number): Observable<TechnicianResponse[]> {
    return this.http.get<TechnicianResponse[]>(
      `${environment.apiUrl}/workshops/${workshopId}/technicians`
    );
  }

  create(payload: CreateTechnicianRequest): Observable<TechnicianResponse> {
    return this.http.post<TechnicianResponse>(`${environment.apiUrl}/technicians`, payload);
  }

  activate(technicianId: number): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/technicians/${technicianId}/activate`, {});
  }

  deactivate(technicianId: number): Observable<void> {
    return this.http.post<void>(
      `${environment.apiUrl}/technicians/${technicianId}/deactivate`,
      {}
    );
  }
}
