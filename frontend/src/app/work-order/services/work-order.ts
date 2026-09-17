import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type WorkOrderStatus =
  | 'SCHEDULED'
  | 'IN_PROGRESS'
  | 'AWAITING_PARTS'
  | 'COMPLETED'
  | 'CANCELLED';

export interface WorkOrderResponse {
  id: number;
  workOrderNumber: string;
  bookingId: number;
  status: WorkOrderStatus;
  startedAt: string | null;
  completedAt: string | null;
  odometerAtService: number | null;
  totalCost: number;
}

export interface WorkOrderLabourResponse {
  id: number;
  technicianId: number;
  technicianName: string;
  hours: number;
  rateApplied: number;
  labourCost: number;
}

export interface WorkOrderPartResponse {
  id: number;
  partId: number;
  partCode: string;
  quantity: number;
  unitCost: number;
  lineCost: number;
  movementId: number;
}

export interface WorkOrderDetailsResponse extends WorkOrderResponse {
  labourEntries: WorkOrderLabourResponse[];
  partsUsed: WorkOrderPartResponse[];
}

export interface CompleteWorkOrderRequest {
  odometerAtService: number;
  hoursWorked: number;
}

export interface CreatePartRequirementRequest {
  partId: number;
  quantityRequired: number;
  reason: string;
}

export interface IssueWorkOrderPartRequest {
  partId: number;
  quantity: number;
}

@Injectable({ providedIn: 'root' })
export class WorkOrderService {
  constructor(private http: HttpClient) {}

  getAll(): Observable<WorkOrderResponse[]> {
    return this.http.get<WorkOrderResponse[]>(`${environment.apiUrl}/work-orders`);
  }

  getById(workOrderId: number): Observable<WorkOrderDetailsResponse> {
    return this.http.get<WorkOrderDetailsResponse>(
      `${environment.apiUrl}/work-orders/${workOrderId}`
    );
  }

  start(workOrderId: number): Observable<WorkOrderResponse> {
    return this.http.post<WorkOrderResponse>(
      `${environment.apiUrl}/work-orders/${workOrderId}/start`,
      {}
    );
  }

  complete(
    workOrderId: number,
    payload: CompleteWorkOrderRequest,
    idempotencyKey: string
  ): Observable<WorkOrderResponse> {
    const headers = new HttpHeaders({ 'Idempotency-Key': idempotencyKey });
    return this.http.post<WorkOrderResponse>(
      `${environment.apiUrl}/work-orders/${workOrderId}/complete`,
      payload,
      { headers }
    );
  }

  awaitParts(
    workOrderId: number,
    payload: CreatePartRequirementRequest
  ): Observable<WorkOrderResponse> {
    return this.http.post<WorkOrderResponse>(
      `${environment.apiUrl}/work-orders/${workOrderId}/await-parts`,
      payload
    );
  }

  issuePart(
    workOrderId: number,
    payload: IssueWorkOrderPartRequest
  ): Observable<WorkOrderResponse> {
    return this.http.post<WorkOrderResponse>(
      `${environment.apiUrl}/work-orders/${workOrderId}/parts/issue`,
      payload
    );
  }

  resume(workOrderId: number): Observable<WorkOrderResponse> {
    return this.http.post<WorkOrderResponse>(
      `${environment.apiUrl}/work-orders/${workOrderId}/resume`,
      {}
    );
  }
}
