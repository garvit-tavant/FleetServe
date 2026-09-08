package com.example.backend.SLA.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.backend.CapacityAndSchedulingService.repository.HolidayRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkingCalendarRepository;
import com.example.backend.ExecutionService.repository.BookingRepository;
import com.example.backend.ExecutionService.repository.WorkOrderRepository;
import com.example.backend.SLA.repository.BreakdownRequestRepository;
import com.example.backend.SLA.repository.SlaCheckpointRepository;
import com.example.backend.SLA.service.SlaTimeCalculationStrategy;
import com.example.backend.SLA.service.impl.CalendarHoursSlaTimeCalculationStrategy;
import com.example.backend.SLA.service.impl.SlaCalculator;
import com.example.backend.SLA.service.impl.WorkingHoursSlaTimeCalculationStrategy;

/**
 * Spring configuration for SLA clock calculation strategies and calculator.
 *
 * Declares two strategies as beans:
 * - WorkingHoursSlaTimeCalculationStrategy: Respects workshop calendars, holidays, shifts
 * - CalendarHoursSlaTimeCalculationStrategy: Pure 24/7 elapsed time
 *
 * The SlaCalculator bean is instantiated with both strategies, allowing it to
 * compute both clocks. At service/controller level, SlaService decides which
 * strategies to use based on SlaPolicy.calendarBasis.
 */
@Configuration
public class SlaCalculationConfiguration {

    @Bean
    public SlaTimeCalculationStrategy workingHoursSlaTimeCalculationStrategy(
            WorkingCalendarRepository workingCalendarRepository,
            HolidayRepository holidayRepository) {
        return new WorkingHoursSlaTimeCalculationStrategy(workingCalendarRepository, holidayRepository);
    }

    @Bean
    public SlaTimeCalculationStrategy calendarHoursSlaTimeCalculationStrategy() {
        return new CalendarHoursSlaTimeCalculationStrategy();
    }

    @Bean
    public SlaCalculator slaCalculator(
            SlaTimeCalculationStrategy workingHoursSlaTimeCalculationStrategy,
            SlaTimeCalculationStrategy calendarHoursSlaTimeCalculationStrategy,
            SlaCheckpointRepository slaCheckpointRepository,
            BreakdownRequestRepository breakdownRequestRepository,
            BookingRepository bookingRepository,
            WorkOrderRepository workOrderRepository) {
        // Default: Use working-hours for both response and resolution phases.
        // This aligns with spec requirement INV-7 (working-calendar measurement).
        // If a future SlaPolicy needs different strategies per phase or per priority,
        // refactor SlaService to accept the policy and decide which strategy to pass.
        return new SlaCalculator(
                workingHoursSlaTimeCalculationStrategy,
                workingHoursSlaTimeCalculationStrategy,
                slaCheckpointRepository,
                breakdownRequestRepository,
                bookingRepository,
                workOrderRepository
            );
    }

}
