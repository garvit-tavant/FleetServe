import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type AssetStatus = 'ACTIVE' | 'IN_SERVICE' | 'OUT_OF_SERVICE' | 'RETIRED';

export interface AssetSummaryResponse {
  id: number;
  vin: string;
  assetClassCode: string;
  status: AssetStatus;
}

export interface AssetResponse {
  id: number;
  vin: string;
  assetClassId: number;
  assetClassCode: string;
  homeDepotId: number;
  homeDepotCode: string;
  acquisitionDate: string;
  acquisitionOdometerKm: number;
  status: AssetStatus;
}

export interface RegisterAssetRequest {
  vin: string;
  assetClassId: number;
  homeDepotId: number;
  acquisitionDate: string;
  acquisitionOdometerKm: number;
}

export interface ReasonRequest {
  reason: string;
}

@Injectable({ providedIn: 'root' })
export class AssetService {
  constructor(private http: HttpClient) {}

  getAll(): Observable<AssetSummaryResponse[]> {
    return this.http.get<AssetSummaryResponse[]>(`${environment.apiUrl}/assets`);
  }

  getById(id: number): Observable<AssetResponse> {
    return this.http.get<AssetResponse>(`${environment.apiUrl}/assets/${id}`);
  }

  register(payload: RegisterAssetRequest): Observable<AssetResponse> {
    return this.http.post<AssetResponse>(`${environment.apiUrl}/assets`, payload);
  }

  retire(id: number, payload: ReasonRequest): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/assets/${id}/retire`, payload);
  }

  reinstate(id: number, payload: ReasonRequest): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/assets/${id}/reinstate`, payload);
  }
}
