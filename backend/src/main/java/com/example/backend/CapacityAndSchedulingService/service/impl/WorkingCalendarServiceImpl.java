package com.example.backend.CapacityAndSchedulingService.service.impl;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.CapacityAndSchedulingService.dto.workingcalendar.CreateWorkingCalendarRequest;
import com.example.backend.CapacityAndSchedulingService.dto.workingcalendar.UpdateWorkingCalendarRequest;
import com.example.backend.CapacityAndSchedulingService.dto.workingcalendar.WorkingCalendarResponse;
import com.example.backend.CapacityAndSchedulingService.entity.WorkingCalendar;
import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import com.example.backend.CapacityAndSchedulingService.mapper.CapacitySchedulingMapper;
import com.example.backend.CapacityAndSchedulingService.repository.WorkingCalendarRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkshopRepository;
import com.example.backend.CapacityAndSchedulingService.service.WorkingCalendarService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class WorkingCalendarServiceImpl
        implements WorkingCalendarService {

    private final WorkingCalendarRepository workingCalendarRepository;
    private final WorkshopRepository workshopRepository;
    private final CapacitySchedulingMapper mapper;

    public WorkingCalendarServiceImpl(
            WorkingCalendarRepository workingCalendarRepository,
            WorkshopRepository workshopRepository,
            CapacitySchedulingMapper mapper
    ) {
        this.workingCalendarRepository =
                workingCalendarRepository;
        this.workshopRepository =
                workshopRepository;
        this.mapper =
                mapper;
    }

    /**
     * Creates one working-calendar entry for a Workshop.
     *
     * A Workshop can have only one calendar entry
     * for each day of the week.
     */
    @Override
    @Transactional
    public WorkingCalendarResponse createWorkingCalendar(
            Long workshopId,
            CreateWorkingCalendarRequest request
    ) {
        validateWorkshopId(workshopId);
        validateCreateRequest(request);

        Workshop workshop =
                getWorkshopEntity(workshopId);

        if (!Boolean.TRUE.equals(workshop.getActive())) {
            throw new BusinessValidationException(
                    "Cannot create a working calendar for inactive workshop "
                            + workshopId
            );
        }

        boolean calendarAlreadyExists =
                workingCalendarRepository
                        .existsByWorkshop_IdAndDayOfWeek(
                                workshopId,
                                request.getDayOfWeek()
                        );

        if (calendarAlreadyExists) {
            throw new DuplicateResourceException(
                    "Working calendar already exists for workshop "
                            + workshopId
                            + " and day of week "
                            + request.getDayOfWeek()
            );
        }

        WorkingCalendar workingCalendar =
                new WorkingCalendar();

        workingCalendar.setWorkshop(workshop);
        workingCalendar.setDayOfWeek(
                request.getDayOfWeek()
        );
        workingCalendar.setOpenTime(
                request.getOpenTime()
        );
        workingCalendar.setCloseTime(
                request.getCloseTime()
        );

        WorkingCalendar savedWorkingCalendar =
                workingCalendarRepository.save(
                        workingCalendar
                );

        return mapper.toWorkingCalendarResponse(
                savedWorkingCalendar
        );
    }

    /**
     * Returns a specific WorkingCalendar entry.
     *
     * Also verifies that the calendar entry belongs
     * to the Workshop provided in the URL.
     */
    @Override
    public WorkingCalendarResponse getWorkingCalendar(
            Long workshopId,
            Long calendarId
    ) {
        WorkingCalendar workingCalendar =
                getWorkingCalendarEntity(
                        workshopId,
                        calendarId
                );

        return mapper.toWorkingCalendarResponse(
                workingCalendar
        );
    }

    /**
     * Returns all WorkingCalendar entries for a Workshop,
     * ordered from Monday to Sunday.
     */
    @Override
    public List<WorkingCalendarResponse>
    getWorkingCalendarsForWorkshop(
            Long workshopId
    ) {
        validateWorkshopId(workshopId);

        getWorkshopEntity(workshopId);

        return workingCalendarRepository
                .findByWorkshop_IdOrderByDayOfWeek(
                        workshopId
                )
                .stream()
                .map(mapper::toWorkingCalendarResponse)
                .toList();
    }

    /**
     * Updates the opening and closing times of an existing
     * WorkingCalendar entry.
     *
     * The day of week is not changed because it identifies
     * the calendar entry within the Workshop.
     */
    @Override
    @Transactional
    public WorkingCalendarResponse updateWorkingCalendar(
            Long workshopId,
            Long calendarId,
            UpdateWorkingCalendarRequest request
    ) {
        validateUpdateRequest(request);

        WorkingCalendar workingCalendar =
                getWorkingCalendarEntity(
                        workshopId,
                        calendarId
                );

        workingCalendar.setOpenTime(
                request.getOpenTime()
        );

        workingCalendar.setCloseTime(
                request.getCloseTime()
        );

        /*
         * An explicit save is valid here.
         *
         * Because workingCalendar is a managed entity inside
         * a transaction, Hibernate dirty checking would also
         * persist these changes without calling save().
         */
        WorkingCalendar updatedWorkingCalendar =
                workingCalendarRepository.save(
                        workingCalendar
                );

        return mapper.toWorkingCalendarResponse(
                updatedWorkingCalendar
        );
    }

    /**
     * Deletes a WorkingCalendar entry.
     *
     * When Booking is implemented, this operation should
     * be re-evaluated to ensure deleting the entry does not
     * invalidate active or future bookings.
     */
    @Override
    @Transactional
    public void deleteWorkingCalendar(
            Long workshopId,
            Long calendarId
    ) {
        WorkingCalendar workingCalendar =
                getWorkingCalendarEntity(
                        workshopId,
                        calendarId
                );

        workingCalendarRepository.delete(
                workingCalendar
        );
    }

    private WorkingCalendar getWorkingCalendarEntity(
            Long workshopId,
            Long calendarId
    ) {
        validateWorkshopId(workshopId);
        validateCalendarId(calendarId);

        getWorkshopEntity(workshopId);

        WorkingCalendar workingCalendar =
                workingCalendarRepository
                        .findById(calendarId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Working calendar not found with id "
                                                        + calendarId
                                        )
                        );

        if (!workingCalendar
                .getWorkshop()
                .getId()
                .equals(workshopId)) {

            throw new ResourceNotFoundException(
                    "Working calendar "
                            + calendarId
                            + " does not belong to workshop "
                            + workshopId
            );
        }

        return workingCalendar;
    }

    private Workshop getWorkshopEntity(
            Long workshopId
    ) {
        return workshopRepository
                .findById(workshopId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Workshop not found with id "
                                                + workshopId
                                )
                );
    }

    private void validateCreateRequest(
            CreateWorkingCalendarRequest request
    ) {
        if (request == null) {
            throw new BusinessValidationException(
                    "Create working-calendar request is required"
            );
        }

        validateDayOfWeek(
                request.getDayOfWeek()
        );

        validateWorkingHours(
                request.getOpenTime(),
                request.getCloseTime()
        );
    }

    private void validateUpdateRequest(
            UpdateWorkingCalendarRequest request
    ) {
        if (request == null) {
            throw new BusinessValidationException(
                    "Update working-calendar request is required"
            );
        }

        validateWorkingHours(
                request.getOpenTime(),
                request.getCloseTime()
        );
    }

    private void validateWorkingHours(
            LocalTime openTime,
            LocalTime closeTime
    ) {
        if (openTime == null) {
            throw new BusinessValidationException(
                    "Open time is required"
            );
        }

        if (closeTime == null) {
            throw new BusinessValidationException(
                    "Close time is required"
            );
        }

        if (!closeTime.isAfter(openTime)) {
            throw new BusinessValidationException(
                    "Close time must be after open time"
            );
        }
    }

    private void validateDayOfWeek(
            Short dayOfWeek
    ) {
        if (dayOfWeek == null) {
            throw new BusinessValidationException(
                    "Day of week is required"
            );
        }

        if (dayOfWeek < 1
                || dayOfWeek > 7) {

            throw new BusinessValidationException(
                    "Day of week must be between 1 and 7"
            );
        }
    }

    private void validateWorkshopId(
            Long workshopId
    ) {
        if (workshopId == null
                || workshopId <= 0) {

            throw new BusinessValidationException(
                    "Workshop ID must be a positive number"
            );
        }
    }

    private void validateCalendarId(
            Long calendarId
    ) {
        if (calendarId == null
                || calendarId <= 0) {

            throw new BusinessValidationException(
                    "Working calendar ID must be a positive number"
            );
        }
    }
}