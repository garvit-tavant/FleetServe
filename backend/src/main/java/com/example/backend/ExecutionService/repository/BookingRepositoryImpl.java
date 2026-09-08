package com.example.backend.ExecutionService.repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import com.example.backend.ExecutionService.repository.projection.BookedSlotProjection;

public class BookingRepositoryImpl implements BookingRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public long insertBooking(
            long assetId,
            long workshopId,
            long bayId,
            long technicianId,
            OffsetDateTime slotStart,
            OffsetDateTime slotEnd,
            String kind,
            Long maintenancePlanId,
            Long breakdownRequestId,
            String status) {

        Query query = entityManager.createNativeQuery("""
                INSERT INTO booking
                    (asset_id, workshop_id, bay_id, technician_id, slot, kind,
                     maintenance_plan_id, breakdown_request_id, status)
                VALUES
                    (:assetId, :workshopId, :bayId, :technicianId,
                     tstzrange(CAST(:slotStart AS timestamptz),
                               CAST(:slotEnd AS timestamptz), '[)'),
                     :kind, :maintenancePlanId, :breakdownRequestId, :status)
                RETURNING id
                """);

        query.setParameter("assetId", assetId);
        query.setParameter("workshopId", workshopId);
        query.setParameter("bayId", bayId);
        query.setParameter("technicianId", technicianId);
        query.setParameter("slotStart", slotStart.toString());
        query.setParameter("slotEnd", slotEnd.toString());
        query.setParameter("kind", kind);
        query.setParameter("maintenancePlanId", maintenancePlanId);
        query.setParameter("breakdownRequestId", breakdownRequestId);
        query.setParameter("status", status);

        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public List<BookedSlotProjection> findOccupyingSlots(
            long workshopId,
            OffsetDateTime from,
            OffsetDateTime to) {

        Query query = entityManager.createNativeQuery("""
                SELECT b.id, b.bay_id, b.technician_id, lower(b.slot), upper(b.slot)
                FROM booking b
                WHERE b.workshop_id = :workshopId
                  AND b.status IN ('HELD', 'CONFIRMED')
                  AND b.slot && tstzrange(CAST(:from AS timestamptz),
                                          CAST(:to AS timestamptz), '[)')
                ORDER BY lower(b.slot), b.id
                """);

        query.setParameter("workshopId", workshopId);
        query.setParameter("from", from.toString());
        query.setParameter("to", to.toString());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<BookedSlotProjection> slots = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            slots.add(new BookedSlotProjection(
                    ((Number) row[0]).longValue(),
                    row[1] == null ? null : ((Number) row[1]).longValue(),
                    row[2] == null ? null : ((Number) row[2]).longValue(),
                    toOffsetDateTime(row[3]),
                    toOffsetDateTime(row[4])));
        }
        return slots;
    }

    private static OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant().atOffset(java.time.ZoneOffset.UTC);
        }
        if (value instanceof java.time.Instant instant) {
            return instant.atOffset(java.time.ZoneOffset.UTC);
        }
        throw new IllegalStateException(
                "Unexpected timestamp type from the database: " + value.getClass());
    }
}
