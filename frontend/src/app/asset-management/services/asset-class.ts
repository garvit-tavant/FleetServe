import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AssetClassResponse {
  id: number;
  code: string;
  description: string;
}

@Injectable({ providedIn: 'root' })
export class AssetClassService {
  constructor(private http: HttpClient) {}

  getAll(): Observable<AssetClassResponse[]> {
    return this.http.get<AssetClassResponse[]>(`${environment.apiUrl}/asset-classes`);
  }
}
