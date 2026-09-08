package com.example.backend.SLA.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;

import org.hibernate.annotations.Formula;

import com.example.backend.CapacityAndSchedulingService.entity.Workshop;

/**
 * A reserved workshop slot, for either preventive or corrective work.
 *
 * <p>The {@code slot} column is a PostgreSQL {@code tstzrange} carrying the
 * exclusion constraints that make double booking structurally impossible
 * (INV-1). Hibernate has no native mapping for a range type, so the column is
 * deliberately <em>not</em> mapped as a writable field: it is read through
 * {@code lower()} and {@code upper()} formulas, and written by the native insert
 * in {@code BookingRepository}. That keeps the database as the arbiter of
 * overlap rather than moving the check into Java, which the specification
 * explicitly warns against.
 */
@Entity
@Table(name = "booking")
public class Booking {

    public static final String KIND_PREVENTIVE = "PREVENTIVE";
    public static final String KIND_CORRECTIVE = "CORRECTIVE";

    public static final String STATUS_HELD = "HELD";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_COMPLETED = "COMPLETED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workshop_id", nullable = false)
    private Workshop workshop;

    @Column(name = "bay_id", nullable = false)
    private Long bayId;

    @Column(name = "technician_id", nullable = false)
    private Long technicianId;

    // Read-only projections of the tstzrange bounds. The range is half-open, so
    // slotEnd is exclusive.
    @Formula("lower(slot)")
    private OffsetDateTime slotStart;

    @Formula("upper(slot)")
    private OffsetDateTime slotEnd;

    @Column(name = "kind", nullable = false, length = 20)
    private String kind;

    /** Set for PREVENTIVE bookings only; null for CORRECTIVE. */
    @Column(name = "maintenance_plan_id")
    private Long maintenancePlanId;

    /*
     * Set for CORRECTIVE bookings only; null for PREVENTIVE.
     *
     * ck_booking_reference_by_kind requires exactly one of this and
     * maintenance_plan_id to be present, so this association must be optional.
     * It was previously optional = false with nullable = false, which made every
     * preventive booking impossible to persist.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "breakdown_request_id", nullable = true)
    private BreakdownRequest breakdownRequest;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public Booking() {
    }

    public boolean isPreventive() {
        return KIND_PREVENTIVE.equals(kind);
    }

    public boolean isCorrective() {
        return KIND_CORRECTIVE.equals(kind);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public Workshop getWorkshop() {
        return workshop;
    }

    public void setWorkshop(Workshop workshop) {
        this.workshop = workshop;
    }

    public Long getBayId() {
        return bayId;
    }

    public void setBayId(Long bayId) {
        this.bayId = bayId;
    }

    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }

    public OffsetDateTime getSlotStart() {
        return slotStart;
    }

    public OffsetDateTime getSlotEnd() {
        return slotEnd;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Long getMaintenancePlanId() {
        return maintenancePlanId;
    }

    public void setMaintenancePlanId(Long maintenancePlanId) {
        this.maintenancePlanId = maintenancePlanId;
    }

    public BreakdownRequest getBreakdownRequest() {
        return breakdownRequest;
    }

    public void setBreakdownRequest(BreakdownRequest breakdownRequest) {
        this.breakdownRequest = breakdownRequest;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
