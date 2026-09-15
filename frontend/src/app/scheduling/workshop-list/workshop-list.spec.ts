import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WorkshopList } from './workshop-list';

describe('WorkshopList', () => {
  let component: WorkshopList;
  let fixture: ComponentFixture<WorkshopList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WorkshopList],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkshopList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
