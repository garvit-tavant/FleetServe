import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WorkOrderList } from './work-order-list';

describe('WorkOrderList', () => {
  let component: WorkOrderList;
  let fixture: ComponentFixture<WorkOrderList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WorkOrderList],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkOrderList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
