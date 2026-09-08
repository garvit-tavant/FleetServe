package com.example.backend.SLA.clock;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Supplies the application clock.
 *
 * <p>The specification requires the clock to be injected rather than read
 * statically, because preventive-maintenance due dates and service-level clocks
 * are both time-dependent and cannot otherwise be tested without waiting for
 * real time to pass. Production uses UTC, matching the TIMESTAMPTZ storage
 * convention; tests substitute {@link Clock#fixed}.
 */
@Configuration
public class ClockConfiguration {

    @Bean
    @Primary
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
