package com.example.backend.CapacityAndSchedulingService.service.impl;

import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.CapacityAndSchedulingService.dto.technician.CreateTechnicianRequest;
import com.example.backend.CapacityAndSchedulingService.dto.technician.TechnicianResponse;
import com.example.backend.CapacityAndSchedulingService.entity.Technician;
import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import com.example.backend.CapacityAndSchedulingService.mapper.CapacitySchedulingMapper;
import com.example.backend.CapacityAndSchedulingService.repository.TechnicianRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkshopRepository;
import com.example.backend.CapacityAndSchedulingService.service.TechnicianService;
import com.example.backend.SecurityService.entity.AppUser;
import com.example.backend.SecurityService.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TechnicianServiceImpl
        implements TechnicianService {

    private final TechnicianRepository technicianRepository;
    private final UserRepository appUserRepository;
    private final WorkshopRepository workshopRepository;
    private final CapacitySchedulingMapper mapper;

    public TechnicianServiceImpl(
            TechnicianRepository technicianRepository,
            UserRepository appUserRepository,
            WorkshopRepository workshopRepository,
            CapacitySchedulingMapper mapper
    ) {
        this.technicianRepository = technicianRepository;
        this.appUserRepository = appUserRepository;
        this.workshopRepository = workshopRepository;
        this.mapper = mapper;
    }

    /**
     * Creates a Technician profile for an existing application user.
     *
     * One application user may have at most one Technician profile.
     * The assigned Workshop must exist and must be active.
     */
    @Override
    @Transactional
    public TechnicianResponse createTechnician(
            CreateTechnicianRequest request
    ) {
        validateCreateTechnicianRequest(request);

        Long appUserId = request.getAppUserId();
        Long workshopId = request.getWorkshopId();

        if (technicianRepository.existsByAppUser_Id(appUserId)) {
            throw new DuplicateResourceException(
                    "A technician profile already exists for app user "
                            + appUserId
            );
        }

        AppUser appUser = getAppUserEntity(appUserId);

        Workshop workshop =
                getWorkshopEntity(workshopId);

        if (!Boolean.TRUE.equals(workshop.getActive())) {
            throw new BusinessValidationException(
                    "Cannot assign technician to inactive workshop "
                            + workshopId
            );
        }

        Technician technician = new Technician();

        technician.setAppUser(appUser);
        technician.setWorkshop(workshop);
        technician.setHourlyRate(request.getHourlyRate());
        technician.setActive(true);

        Technician savedTechnician =
                technicianRepository.save(technician);

        return mapper.toTechnicianResponse(
                savedTechnician
        );
    }

    /**
     * Returns a Technician by its database identifier.
     */
    @Override
    public TechnicianResponse getTechnician(
            Long technicianId
    ) {
        Technician technician =
                getTechnicianEntity(technicianId);

        return mapper.toTechnicianResponse(
                technician
        );
    }

    /**
     * Returns all Technicians, including active and inactive records.
     */
    @Override
    public List<TechnicianResponse>
    getAllTechnicians() {
        return technicianRepository
                .findAll()
                .stream()
                .map(mapper::toTechnicianResponse)
                .toList();
    }

    /**
     * Returns all Technicians assigned to a Workshop.
     *
     * Both active and inactive Technicians are returned.
     */
    @Override
    public List<TechnicianResponse>
    getTechniciansByWorkshop(
            Long workshopId
    ) {
        validateWorkshopId(workshopId);

        getWorkshopEntity(workshopId);

        return technicianRepository
                .findByWorkshop_Id(workshopId)
                .stream()
                .map(mapper::toTechnicianResponse)
                .toList();
    }

    /**
     * Activates an inactive Technician.
     *
     * A Technician cannot be activated while the assigned
     * Workshop is inactive.
     */
    @Override
    @Transactional
    public void activateTechnician(
            Long technicianId
    ) {
        Technician technician =
                getTechnicianEntity(technicianId);

        if (Boolean.TRUE.equals(technician.getActive())) {
            throw new BusinessValidationException(
                    "Technician is already active"
            );
        }

        Workshop workshop =
                technician.getWorkshop();

        if (!Boolean.TRUE.equals(workshop.getActive())) {
            throw new BusinessValidationException(
                    "Cannot activate technician "
                            + technicianId
                            + " because workshop "
                            + workshop.getId()
                            + " is inactive"
            );
        }

        technician.setActive(true);

        /*
         * An explicit save is valid.
         *
         * Because the entity is managed inside this transaction,
         * Hibernate dirty checking would also update it automatically.
         */
        technicianRepository.save(technician);
    }

    /**
     * Deactivates an active Technician.
     *
     * When Booking is implemented, this operation should be blocked
     * if the Technician has active or future bookings.
     */
    @Override
    @Transactional
    public void deactivateTechnician(
            Long technicianId
    ) {
        Technician technician =
                getTechnicianEntity(technicianId);

        if (Boolean.FALSE.equals(technician.getActive())) {
            throw new BusinessValidationException(
                    "Technician is already inactive"
            );
        }

        /*
         * Future Booking validation:
         *
         * Reject deactivation if this Technician is assigned
         * to an active or future Booking.
         */

        technician.setActive(false);

        technicianRepository.save(technician);
    }

    private Technician getTechnicianEntity(
            Long technicianId
    ) {
        validateTechnicianId(technicianId);

        return technicianRepository
                .findById(technicianId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Technician not found with id "
                                        + technicianId
                        )
                );
    }

    private AppUser getAppUserEntity(
            Long appUserId
    ) {
        validateAppUserId(appUserId);

        return appUserRepository
                .findById(appUserId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "App user not found with id "
                                        + appUserId
                        )
                );
    }

    private Workshop getWorkshopEntity(
            Long workshopId
    ) {
        validateWorkshopId(workshopId);

        return workshopRepository
                .findById(workshopId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workshop not found with id "
                                        + workshopId
                        )
                );
    }

    private void validateCreateTechnicianRequest(
            CreateTechnicianRequest request
    ) {
        if (request == null) {
            throw new BusinessValidationException(
                    "Create technician request is required"
            );
        }

        validateAppUserId(request.getAppUserId());
        validateWorkshopId(request.getWorkshopId());

        if (request.getHourlyRate() == null) {
            throw new BusinessValidationException(
                    "Hourly rate is required"
            );
        }

        if (request.getHourlyRate()
                .compareTo(BigDecimal.ZERO) < 0) {

            throw new BusinessValidationException(
                    "Hourly rate cannot be negative"
            );
        }
    }

    private void validateTechnicianId(
            Long technicianId
    ) {
        if (technicianId == null
                || technicianId <= 0) {

            throw new BusinessValidationException(
                    "Technician ID must be a positive number"
            );
        }
    }

    private void validateAppUserId(
            Long appUserId
    ) {
        if (appUserId == null
                || appUserId <= 0) {

            throw new BusinessValidationException(
                    "App user ID must be a positive number"
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
}