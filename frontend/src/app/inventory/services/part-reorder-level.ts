import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PartReorderLevelResponse {
  id: number;
  partId: number;
  partNumber: string;
  workshopId: number;
  workshopCode: string;
  reorderLevel: number;
  version: number;
}

export interface CreateOrUpdatePartReorderLevelRequest {
  partId: number;
  workshopId: number;
  reorderLevel: number;
}

@Injectable({ providedIn: 'root' })
export class PartReorderLevelService {
  private baseUrl = `${environment.apiUrl}/v1/reorder-levels`;

  constructor(private http: HttpClient) {}

  create(
    payload: CreateOrUpdatePartReorderLevelRequest
  ): Observable<PartReorderLevelResponse> {
    return this.http.post<PartReorderLevelResponse>(this.baseUrl, payload);
  }

  update(
    id: number,
    payload: CreateOrUpdatePartReorderLevelRequest,
    version: number
  ): Observable<PartReorderLevelResponse> {
    return this.http.put<PartReorderLevelResponse>(`${this.baseUrl}/${id}`, payload, {
      params: { version },
    });
  }

  getById(id: number): Observable<PartReorderLevelResponse> {
    return this.http.get<PartReorderLevelResponse>(`${this.baseUrl}/${id}`);
  }

  getByPart(partId: number): Observable<PartReorderLevelResponse[]> {
    return this.http.get<PartReorderLevelResponse[]>(`${this.baseUrl}/part/${partId}`);
  }

  getByWorkshop(workshopId: number): Observable<PartReorderLevelResponse[]> {
    return this.http.get<PartReorderLevelResponse[]>(`${this.baseUrl}/workshop/${workshopId}`);
  }

  delete(id: number, version: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`, { params: { version } });
  }
}
