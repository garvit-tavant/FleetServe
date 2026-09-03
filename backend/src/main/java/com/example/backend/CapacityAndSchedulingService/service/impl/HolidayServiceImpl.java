package com.example.backend.CapacityAndSchedulingService.service.impl;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.CapacityAndSchedulingService.dto.holiday.CreateGlobalHolidayRequest;
import com.example.backend.CapacityAndSchedulingService.dto.holiday.CreateWorkshopHolidayRequest;
import com.example.backend.CapacityAndSchedulingService.dto.holiday.HolidayResponse;
import com.example.backend.CapacityAndSchedulingService.entity.Holiday;
import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import com.example.backend.CapacityAndSchedulingService.mapper.CapacitySchedulingMapper;
import com.example.backend.CapacityAndSchedulingService.repository.HolidayRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkshopRepository;
import com.example.backend.CapacityAndSchedulingService.service.HolidayService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class HolidayServiceImpl
        implements HolidayService {

    private final HolidayRepository holidayRepository;
    private final WorkshopRepository workshopRepository;
    private final CapacitySchedulingMapper mapper;

    public HolidayServiceImpl(
            HolidayRepository holidayRepository,
            WorkshopRepository workshopRepository,
            CapacitySchedulingMapper mapper
    ) {
        this.holidayRepository =
                holidayRepository;
        this.workshopRepository =
                workshopRepository;
        this.mapper =
                mapper;
    }

    /**
     * Creates a holiday that applies only to one Workshop.
     */
    @Override
    @Transactional
    public HolidayResponse createWorkshopHoliday(
            Long workshopId,
            CreateWorkshopHolidayRequest request
    ) {
        validateWorkshopId(workshopId);
        validateWorkshopHolidayRequest(request);

        Workshop workshop =
                getWorkshopEntity(workshopId);

        if (!Boolean.TRUE.equals(workshop.getActive())) {
            throw new BusinessValidationException(
                    "Cannot create a holiday for inactive workshop "
                            + workshopId
            );
        }

        boolean holidayAlreadyExists =
                holidayRepository
                        .existsByWorkshop_IdAndHolidayDate(
                                workshopId,
                                request.getHolidayDate()
                        );

        if (holidayAlreadyExists) {
            throw new DuplicateResourceException(
                    "A holiday already exists for workshop "
                            + workshopId
                            + " on "
                            + request.getHolidayDate()
            );
        }

        Holiday holiday =
                new Holiday();

        holiday.setWorkshop(workshop);
        holiday.setHolidayDate(
                request.getHolidayDate()
        );
        holiday.setDescription(
                normalizeDescription(
                        request.getDescription()
                )
        );

        Holiday savedHoliday =
                holidayRepository.save(holiday);

        return mapper.toHolidayResponse(
                savedHoliday
        );
    }

    /**
     * Returns all holidays configured specifically
     * for a Workshop.
     *
     * Global holidays are not included in this result.
     */
    @Override
    public List<HolidayResponse> getWorkshopHolidays(
            Long workshopId
    ) {
        validateWorkshopId(workshopId);

        getWorkshopEntity(workshopId);

        return holidayRepository
                .findByWorkshop_IdOrderByHolidayDate(
                        workshopId
                )
                .stream()
                .map(mapper::toHolidayResponse)
                .toList();
    }

    /**
     * Deletes a workshop-specific holiday.
     *
     * The holiday must belong to the Workshop specified
     * in the request URL.
     */
    @Override
    @Transactional
    public void deleteWorkshopHoliday(
            Long workshopId,
            Long holidayId
    ) {
        validateWorkshopId(workshopId);
        validateHolidayId(holidayId);

        getWorkshopEntity(workshopId);

        Holiday holiday =
                getHolidayEntity(holidayId);

        if (holiday.getWorkshop() == null) {
            throw new BusinessValidationException(
                    "Holiday "
                            + holidayId
                            + " is a global holiday and cannot be deleted "
                            + "through a workshop-specific route"
            );
        }

        if (!holiday
                .getWorkshop()
                .getId()
                .equals(workshopId)) {

            throw new ResourceNotFoundException(
                    "Holiday "
                            + holidayId
                            + " does not belong to workshop "
                            + workshopId
            );
        }

        /*
         * When Booking is implemented, check whether removing this
         * holiday affects active or future bookings before deletion.
         */
        holidayRepository.delete(holiday);
    }

    /**
     * Creates a global holiday that applies to every Workshop.
     *
     * A global holiday has a null workshop reference.
     */
    @Override
    @Transactional
    public HolidayResponse createGlobalHoliday(
            CreateGlobalHolidayRequest request
    ) {
        validateGlobalHolidayRequest(request);

        boolean holidayAlreadyExists =
                holidayRepository
                        .existsByWorkshopIsNullAndHolidayDate(
                                request.getHolidayDate()
                        );

        if (holidayAlreadyExists) {
            throw new DuplicateResourceException(
                    "A global holiday already exists on "
                            + request.getHolidayDate()
            );
        }

        Holiday holiday =
                new Holiday();

        holiday.setWorkshop(null);
        holiday.setHolidayDate(
                request.getHolidayDate()
        );
        holiday.setDescription(
                normalizeDescription(
                        request.getDescription()
                )
        );

        Holiday savedHoliday =
                holidayRepository.save(holiday);

        return mapper.toHolidayResponse(
                savedHoliday
        );
    }

    /**
     * Returns all global holidays.
     */
    @Override
    public List<HolidayResponse> getGlobalHolidays() {
        return holidayRepository
                .findByWorkshopIsNullOrderByHolidayDate()
                .stream()
                .map(mapper::toHolidayResponse)
                .toList();
    }

    /**
     * Deletes a global holiday.
     *
     * A workshop-specific holiday cannot be deleted
     * through this operation.
     */
    @Override
    @Transactional
    public void deleteGlobalHoliday(
            Long holidayId
    ) {
        validateHolidayId(holidayId);

        Holiday holiday =
                getHolidayEntity(holidayId);

        if (holiday.getWorkshop() != null) {
            throw new BusinessValidationException(
                    "Holiday "
                            + holidayId
                            + " is workshop-specific and cannot be deleted "
                            + "through the global holiday route"
            );
        }

        /*
         * When Booking is implemented, check whether removing this
         * holiday affects active or future bookings before deletion.
         */
        holidayRepository.delete(holiday);
    }

    private Holiday getHolidayEntity(
            Long holidayId
    ) {
        return holidayRepository
                .findById(holidayId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Holiday not found with id "
                                        + holidayId
                        )
                );
    }

    private Workshop getWorkshopEntity(
            Long workshopId
    ) {
        return workshopRepository
                .findById(workshopId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workshop not found with id "
                                        + workshopId
                        )
                );
    }

    private void validateWorkshopHolidayRequest(
            CreateWorkshopHolidayRequest request
    ) {
        if (request == null) {
            throw new BusinessValidationException(
                    "Create workshop-holiday request is required"
            );
        }

        validateHolidayFields(
                request.getHolidayDate(),
                request.getDescription()
        );
    }

    private void validateGlobalHolidayRequest(
            CreateGlobalHolidayRequest request
    ) {
        if (request == null) {
            throw new BusinessValidationException(
                    "Create global-holiday request is required"
            );
        }

        validateHolidayFields(
                request.getHolidayDate(),
                request.getDescription()
        );
    }

    private void validateHolidayFields(
            LocalDate holidayDate,
            String description
    ) {
        if (holidayDate == null) {
            throw new BusinessValidationException(
                    "Holiday date is required"
            );
        }

        if (description == null
                || description.isBlank()) {

            throw new BusinessValidationException(
                    "Holiday description is required"
            );
        }

        if (description.trim().length() > 255) {
            throw new BusinessValidationException(
                    "Holiday description cannot exceed 255 characters"
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

    private void validateHolidayId(
            Long holidayId
    ) {
        if (holidayId == null
                || holidayId <= 0) {

            throw new BusinessValidationException(
                    "Holiday ID must be a positive number"
            );
        }
    }

    private String normalizeDescription(
            String description
    ) {
        return description.trim();
    }
}