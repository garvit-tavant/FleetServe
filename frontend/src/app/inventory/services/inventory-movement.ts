import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from './part';

export type MovementType =
  | 'RECEIPT'
  | 'RETURN'
  | 'TRANSFER_IN'
  | 'ISSUE'
  | 'TRANSFER_OUT'
  | 'ADJUSTMENT';

export interface InventoryMovementResponse {
  id: number;
  partId: number;
  partNumber: string;
  workshopId: number;
  workshopCode: string;
  movementType: MovementType;
  signedQuantity: number;
  unitCost: number;
  transferReference: string | null;
  reason: string | null;
  recordedBy: string;
  occurredAt: string;
}

export interface ReceiveStockRequest {
  partId: number;
  workshopId: number;
  quantity: number;
  unitCost: number;
  reason?: string;
}

export interface AdjustStockRequest {
  partId: number;
  workshopId: number;
  adjustmentQuantity: number;
  reason: string;
}

export interface IssuePartRequest {
  partId: number;
  workshopId: number;
  quantity: number;
  unitCost: number;
  reason?: string;
}

export interface ReturnPartRequest {
  partId: number;
  workshopId: number;
  quantity: number;
  unitCost: number;
  reason?: string;
}

@Injectable({ providedIn: 'root' })
export class InventoryMovementService {
  private baseUrl = `${environment.apiUrl}/v1/inventory`;

  constructor(private http: HttpClient) {}

  receiveStock(payload: ReceiveStockRequest): Observable<InventoryMovementResponse> {
    return this.http.post<InventoryMovementResponse>(`${this.baseUrl}/receive`, payload);
  }

  issuePart(payload: IssuePartRequest): Observable<InventoryMovementResponse> {
    return this.http.post<InventoryMovementResponse>(`${this.baseUrl}/issue`, payload);
  }

  returnPart(payload: ReturnPartRequest): Observable<InventoryMovementResponse> {
    return this.http.post<InventoryMovementResponse>(`${this.baseUrl}/return`, payload);
  }

  adjustStock(payload: AdjustStockRequest): Observable<InventoryMovementResponse> {
    return this.http.post<InventoryMovementResponse>(`${this.baseUrl}/adjust`, payload);
  }

  getMovementsForPart(
    partId: number,
    page = 0,
    size = 50
  ): Observable<PageResponse<InventoryMovementResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<InventoryMovementResponse>>(
      `${this.baseUrl}/movements/part/${partId}`,
      { params }
    );
  }

  getMovementsForWorkshop(
    workshopId: number,
    page = 0,
    size = 50
  ): Observable<PageResponse<InventoryMovementResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<InventoryMovementResponse>>(
      `${this.baseUrl}/movements/workshop/${workshopId}`,
      { params }
    );
  }

  getMovements(
    partId: number,
    workshopId: number,
    page = 0,
    size = 50
  ): Observable<PageResponse<InventoryMovementResponse>> {
    const params = new HttpParams()
      .set('partId', partId)
      .set('workshopId', workshopId)
      .set('page', page)
      .set('size', size);
    return this.http.get<PageResponse<InventoryMovementResponse>>(`${this.baseUrl}/movements`, {
      params,
    });
  }
}
