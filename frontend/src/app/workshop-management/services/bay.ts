import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface BayResponse {
  id: number;
  workshopId: number;
  bayCode: string;
  active: boolean;
}

export interface CreateBayRequest {
  bayCode: string;
}

@Injectable({ providedIn: 'root' })
export class BayService {
  constructor(private http: HttpClient) {}

  getByWorkshop(workshopId: number): Observable<BayResponse[]> {
    return this.http.get<BayResponse[]>(`${environment.apiUrl}/workshops/${workshopId}/bays`);
  }

  getById(bayId: number): Observable<BayResponse> {
    return this.http.get<BayResponse>(`${environment.apiUrl}/bays/${bayId}`);
  }

  create(workshopId: number, payload: CreateBayRequest): Observable<BayResponse> {
    return this.http.post<BayResponse>(`${environment.apiUrl}/workshops/${workshopId}/bays`, payload);
  }

  activate(bayId: number): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/bays/${bayId}/activate`, {});
  }

  deactivate(bayId: number): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/bays/${bayId}/deactivate`, {});
  }
}
