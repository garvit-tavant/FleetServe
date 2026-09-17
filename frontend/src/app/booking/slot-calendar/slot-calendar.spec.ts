import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SlotCalendar } from './slot-calendar';

describe('SlotCalendar', () => {
  let component: SlotCalendar;
  let fixture: ComponentFixture<SlotCalendar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SlotCalendar],
    }).compileComponents();

    fixture = TestBed.createComponent(SlotCalendar);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
