import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PartResponse {
  id: number;
  partNumber: string;
  description: string;
  unitOfMeasure: string;
  standardCost: number;
  active: boolean;
  version: number;
}

export interface CreatePartRequest {
  partNumber: string;
  description: string;
  unitOfMeasure: string;
  standardCost: number;
}

export interface UpdatePartRequest {
  partNumber: string;
  description: string;
  unitOfMeasure: string;
  standardCost: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class PartService {
  private baseUrl = `${environment.apiUrl}/v1/parts`;

  constructor(private http: HttpClient) {}

  getAll(page = 0, size = 50): Observable<PageResponse<PartResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<PartResponse>>(this.baseUrl, { params });
  }

  getById(partId: number): Observable<PartResponse> {
    return this.http.get<PartResponse>(`${this.baseUrl}/${partId}`);
  }

  create(payload: CreatePartRequest): Observable<PartResponse> {
    return this.http.post<PartResponse>(this.baseUrl, payload);
  }

  update(partId: number, payload: UpdatePartRequest): Observable<PartResponse> {
    return this.http.put<PartResponse>(`${this.baseUrl}/${partId}`, payload);
  }

  deactivate(partId: number, version: number): Observable<void> {
    const params = new HttpParams().set('version', version);
    return this.http.delete<void>(`${this.baseUrl}/${partId}`, { params });
  }
}
