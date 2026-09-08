package com.example.backend.ExecutionService.scheduling;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import com.example.backend.CapacityAndSchedulingService.entity.Bay;
import com.example.backend.CapacityAndSchedulingService.entity.BayCapability;
import com.example.backend.CapacityAndSchedulingService.entity.Technician;
import com.example.backend.CapacityAndSchedulingService.entity.TechnicianSkill;
import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import com.example.backend.CapacityAndSchedulingService.entity.WorkingCalendar;
import com.example.backend.CapacityAndSchedulingService.repository.BayRepository;
import com.example.backend.CapacityAndSchedulingService.repository.HolidayRepository;
import com.example.backend.CapacityAndSchedulingService.repository.TechnicianRepository;
import com.example.backend.CapacityAndSchedulingService.repository.TechnicianSkillRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkingCalendarRepository;
import com.example.backend.CapacityAndSchedulingService.repository.WorkshopRepository;
import com.example.backend.ExecutionService.repository.BookingRepository;
import com.example.backend.ExecutionService.repository.projection.BookedSlotProjection;
import com.example.slotengine.model.BayCandidate;
import com.example.slotengine.model.ExistingBooking;
import com.example.slotengine.model.SearchHorizon;
import com.example.slotengine.model.SkillCertification;
import com.example.slotengine.model.TechnicianCandidate;
import com.example.slotengine.model.WorkingDayHours;

/**
 * Loads a workshop's schedule out of the database and into the slot engine's
 * value objects.
 *
 * <p>All persistence for slot searching lives here, which is what lets the
 * engine itself stay a pure function with no repository or clock.
 */
@Component
@Transactional(readOnly = true)
public class WorkshopScheduleLoader {

    private final WorkshopRepository workshopRepository;
    private final BayRepository bayRepository;
    private final TechnicianRepository technicianRepository;
    private final TechnicianSkillRepository technicianSkillRepository;
    private final WorkingCalendarRepository workingCalendarRepository;
    private final HolidayRepository holidayRepository;
    private final BookingRepository bookingRepository;

    public WorkshopScheduleLoader(
            WorkshopRepository workshopRepository,
            BayRepository bayRepository,
            TechnicianRepository technicianRepository,
            TechnicianSkillRepository technicianSkillRepository,
            WorkingCalendarRepository workingCalendarRepository,
            HolidayRepository holidayRepository,
            BookingRepository bookingRepository) {
        this.workshopRepository = workshopRepository;
        this.bayRepository = bayRepository;
        this.technicianRepository = technicianRepository;
        this.technicianSkillRepository = technicianSkillRepository;
        this.workingCalendarRepository = workingCalendarRepository;
        this.holidayRepository = holidayRepository;
        this.bookingRepository = bookingRepository;
    }

    /** Everything the engine needs about one workshop over one horizon. */
    public record WorkshopSchedule(
            Workshop workshop,
            ZoneId zone,
            List<BayCandidate> bays,
            List<TechnicianCandidate> technicians,
            com.example.slotengine.model.WorkingCalendar calendar,
            List<ExistingBooking> bookings) {
    }

    public WorkshopSchedule load(long workshopId, SearchHorizon horizon) {
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Workshop not found with id " + workshopId));

        ZoneId zone = zoneOf(workshop);

        return new WorkshopSchedule(
                workshop,
                zone,
                loadBays(workshopId),
                loadTechnicians(workshopId),
                loadCalendar(workshopId, horizon),
                loadBookings(workshopId, zone, horizon));
    }

    private List<BayCandidate> loadBays(long workshopId) {
        List<Bay> bays = bayRepository.findByWorkshop_Id(workshopId);
        List<BayCandidate> candidates = new ArrayList<>(bays.size());

        for (Bay bay : bays) {
            Set<String> capabilities = bay.getCapabilities().stream()
                    .map(BayCapability::getCapability)
                    .filter(java.util.Objects::nonNull)
                    .map(capability -> capability.getCapabilityCode())
                    .collect(Collectors.toSet());

            candidates.add(new BayCandidate(
                    bay.getId(),
                    Boolean.TRUE.equals(bay.getIsActive()),
                    capabilities));
        }
        return candidates;
    }

    private List<TechnicianCandidate> loadTechnicians(long workshopId) {
        List<Technician> technicians = technicianRepository.findByWorkshop_Id(workshopId);
        List<TechnicianCandidate> candidates = new ArrayList<>(technicians.size());

        for (Technician technician : technicians) {
            List<SkillCertification> certifications =
                    technicianSkillRepository.findByTechnician_Id(technician.getId()).stream()
                            .filter(skill -> skill.getSkill() != null)
                            .map(WorkshopScheduleLoader::toCertification)
                            .toList();

            candidates.add(new TechnicianCandidate(
                    technician.getId(),
                    Boolean.TRUE.equals(technician.getActive()),
                    certifications));
        }
        return candidates;
    }

    private static SkillCertification toCertification(TechnicianSkill skill) {
        return new SkillCertification(
                skill.getSkill().getSkillCode(),
                skill.getValidFrom(),
                skill.getValidTo());
    }

    /**
     * The engine's calendar carries no time zone, because it reasons entirely in
     * workshop-local dates and times. The zone is kept alongside it on
     * {@link WorkshopSchedule} and applied only when converting a chosen slot
     * back into an absolute instant.
     */
    private com.example.slotengine.model.WorkingCalendar loadCalendar(
            long workshopId, SearchHorizon horizon) {
        Map<DayOfWeek, WorkingDayHours> weekly = new EnumMap<>(DayOfWeek.class);

        for (WorkingCalendar row : workingCalendarRepository
                .findByWorkshop_IdOrderByDayOfWeek(workshopId)) {

            if (row.getDayOfWeek() == null
                    || row.getOpenTime() == null
                    || row.getCloseTime() == null) {
                continue;
            }
            // working_calendar.day_of_week uses ISO numbering, 1 = Monday.
            weekly.put(DayOfWeek.of(row.getDayOfWeek()),
                    new WorkingDayHours(row.getOpenTime(), row.getCloseTime()));
        }

        Set<LocalDate> holidays = new HashSet<>(holidayRepository.findHolidayDatesBetween(
                workshopId, horizon.startDate(), horizon.endDateExclusive()));

        return new com.example.slotengine.model.WorkingCalendar(weekly, holidays);
    }

    /**
     * Existing bookings, converted from instants into the workshop's local dates
     * and times because the engine reasons in workshop-local terms.
     *
     * <p>A booking is fetched if it touches the horizon at all, and the window is
     * widened by a day on each side so that a booking running across midnight in
     * the workshop's zone is not missed.
     */
    private List<ExistingBooking> loadBookings(long workshopId, ZoneId zone, SearchHorizon horizon) {
        OffsetDateTime from = horizon.startDate().minusDays(1)
                .atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime to = horizon.endDateExclusive().plusDays(1)
                .atStartOfDay(zone).toOffsetDateTime();

        List<BookedSlotProjection> booked =
                bookingRepository.findOccupyingSlots(workshopId, from, to);

        List<ExistingBooking> bookings = new ArrayList<>(booked.size());
        for (BookedSlotProjection slot : booked) {
            if (slot.slotStart() == null || slot.slotEnd() == null) {
                continue;
            }
            LocalDate date = slot.slotStart().atZoneSameInstant(zone).toLocalDate();
            LocalTime start = slot.slotStart().atZoneSameInstant(zone).toLocalTime();
            LocalTime end = slot.slotEnd().atZoneSameInstant(zone).toLocalTime();

            // A booking may not span two working days, so an end that has wrapped
            // past midnight is clamped to the end of the day it started on.
            if (!end.isAfter(start)) {
                end = LocalTime.MAX;
            }
            bookings.add(new ExistingBooking(
                    date, start, end, slot.bayId(), slot.technicianId()));
        }
        return bookings;
    }

    private static ZoneId zoneOf(Workshop workshop) {
        try {
            return ZoneId.of(workshop.getTimeZone());
        } catch (java.time.DateTimeException | NullPointerException ex) {
            throw new ResourceNotFoundException(
                    "Workshop " + workshop.getId() + " has an unusable time zone: "
                            + workshop.getTimeZone());
        }
    }
}
