package com.example.backend.AssetManagamentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.example.backend.AssetManagamentService.dto.duemaintenance.DueMaintenanceFilter;
import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.repository.DueMaintenanceRepository;
import com.example.backend.AssetManagamentService.service.DueMaintenanceService;
import com.example.backend.AssetManagamentService.service.impl.DueMaintenanceServiceImpl;
import com.example.backend.AssetManagamentService.status.DueStatus;

class DueMaintenanceServiceTest {

    private DueMaintenanceRepository repository;

    @BeforeEach
    void setUp() {
        repository = mock(DueMaintenanceRepository.class);
        when(repository.findDueMaintenance(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(Page.empty());
    }

    private DueMaintenanceService serviceAt(String isoDate) {
        Clock fixed = Clock.fixed(Instant.parse(isoDate + "T09:00:00Z"), ZoneId.of("UTC"));
        return new DueMaintenanceServiceImpl(repository, fixed);
    }

    @Test
    @DisplayName("The evaluation date comes from the injected clock, not the wall clock")
    void evaluationDateComesFromInjectedClock() {
        serviceAt("2026-03-01").getDueMaintenance(
                DueMaintenanceFilter.defaults(), PageRequest.of(0, 20));

        ArgumentCaptor<LocalDate> today = ArgumentCaptor.forClass(LocalDate.class);
        verify(repository).findDueMaintenance(
                today.capture(), any(), any(), any(), any(), any(), anyBoolean(), any());

        assertEquals(LocalDate.of(2026, 3, 1), today.getValue());
    }

    @Test
    @DisplayName("With no override supplied, the query falls back to the stored plan thresholds")
    void noOverrideMeansPlanThresholdsApply() {
        serviceAt("2026-03-01").getDueMaintenance(
                DueMaintenanceFilter.defaults(), PageRequest.of(0, 20));

        ArgumentCaptor<BigDecimal> distance = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<Integer> days = ArgumentCaptor.forClass(Integer.class);
        verify(repository).findDueMaintenance(
                any(), distance.capture(), days.capture(), any(), any(), any(), anyBoolean(), any());

        assertNull(distance.getValue(), "null lets COALESCE pick the plan value");
        assertNull(days.getValue());
    }

    @Test
    @DisplayName("Filters are passed through, with status flattened to its name")
    void filtersArePassedThrough() {
        DueMaintenanceFilter filter = new DueMaintenanceFilter(
                new BigDecimal("250"), 7, 42L, "PASSENGER_CAR", DueStatus.OVERDUE, false);

        serviceAt("2026-03-01").getDueMaintenance(filter, PageRequest.of(0, 20));

        verify(repository).findDueMaintenance(
                any(),
                eq(new BigDecimal("250")),
                eq(7),
                eq(42L),
                eq("PASSENGER_CAR"),
                eq("OVERDUE"),
                eq(false),
                any());
    }

    @Test
    @DisplayName("Page size is capped server-side")
    void pageSizeIsCapped() {
        serviceAt("2026-03-01").getDueMaintenance(
                DueMaintenanceFilter.defaults(), PageRequest.of(0, 5000));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findDueMaintenance(
                any(), any(), any(), any(), any(), any(), anyBoolean(), pageable.capture());

        assertEquals(DueMaintenanceService.MAX_PAGE_SIZE, pageable.getValue().getPageSize());
    }

    @Test
    @DisplayName("Caller sort is dropped so it cannot collide with the native ORDER BY")
    void callerSortIsDropped() {
        serviceAt("2026-03-01").getDueMaintenance(
                DueMaintenanceFilter.defaults(),
                PageRequest.of(0, 20, Sort.by("vin").descending()));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findDueMaintenance(
                any(), any(), any(), any(), any(), any(), anyBoolean(), pageable.capture());

        assertEquals(Sort.unsorted(), pageable.getValue().getSort());
    }

    @Test
    @DisplayName("Negative threshold overrides are rejected")
    void negativeOverridesRejected() {
        DueMaintenanceService service = serviceAt("2026-03-01");
        Pageable page = PageRequest.of(0, 20);

        assertThrows(BusinessValidationException.class, () ->
                service.getDueMaintenance(
                        new DueMaintenanceFilter(new BigDecimal("-1"), null, null, null, null, true),
                        page));

        assertThrows(BusinessValidationException.class, () ->
                service.getDueMaintenance(
                        new DueMaintenanceFilter(null, -1, null, null, null, true),
                        page));
    }
}
