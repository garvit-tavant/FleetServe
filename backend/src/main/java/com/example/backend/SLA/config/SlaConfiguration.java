package com.example.backend.SLA.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
public class SlaConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

}
