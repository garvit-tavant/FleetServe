import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DepotResponse {
  id: number;
  code: string;
  region: string;
  active: boolean;
}

@Injectable({ providedIn: 'root' })
export class DepotService {
  constructor(private http: HttpClient) {}

  getAll(): Observable<DepotResponse[]> {
    return this.http.get<DepotResponse[]>(`${environment.apiUrl}/depots`);
  }
}
