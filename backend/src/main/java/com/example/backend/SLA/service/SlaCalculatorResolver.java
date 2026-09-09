package com.example.backend.SLA.service;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.backend.SLA.dto.SlaBasis;

@Component
public class SlaCalculatorResolver {
    private final Map<SlaBasis, SlaTimeCalculationStrategy> byBasis;

    // mapped calendar time and working calendar time to correct strategies


    public SlaCalculatorResolver(List<SlaTimeCalculationStrategy> strategies) {
        this.byBasis = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(
                        SlaTimeCalculationStrategy::supportedBasis,
                        Function.identity()));
    }


    public SlaTimeCalculationStrategy resolve(SlaBasis calendarBasis) {

        if(calendarBasis == null) {
            calendarBasis = SlaBasis.WORKING_TIME;
        }

        SlaTimeCalculationStrategy strategy = byBasis.get(calendarBasis);
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "No SlaTimeCalculationStrategy registered for calendar_basis '"
                            + calendarBasis + "'. Registered: " + byBasis.keySet());
        }
        return strategy;
}

}
