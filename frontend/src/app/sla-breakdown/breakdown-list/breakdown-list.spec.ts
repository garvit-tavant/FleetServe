import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BreakdownList } from './breakdown-list';

describe('BreakdownList', () => {
  let component: BreakdownList;
  let fixture: ComponentFixture<BreakdownList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BreakdownList],
    }).compileComponents();

    fixture = TestBed.createComponent(BreakdownList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
