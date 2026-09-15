import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DueMaintenanceList } from './due-maintenance-list';

describe('DueMaintenanceList', () => {
  let component: DueMaintenanceList;
  let fixture: ComponentFixture<DueMaintenanceList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DueMaintenanceList],
    }).compileComponents();

    fixture = TestBed.createComponent(DueMaintenanceList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
