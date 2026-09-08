package com.example.backend.ExecutionService.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// Range mapping removed to avoid runtime dependency issues; store as String
//import io.hypersistence.utils.hibernate.type.range.spring.PostgreSQLSpringRangeType;


import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import com.example.backend.SLA.entity.BreakdownRequest;


@Entity
@Table(name = "booking")
public class Booking {
    /*
      id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    asset_id            BIGINT      NOT NULL,
    workshop_id         BIGINT      NOT NULL,
    bay_id              BIGINT      NOT NULL,
    technician_id       BIGINT      NOT NULL,
    slot                TSTZRANGE   NOT NULL,
    kind                VARCHAR(20) NOT NULL,
    maintenance_plan_id BIGINT,
    breakdown_request_id BIGINT,
    status              VARCHAR(30) NOT NULL DEFAULT 'HELD',
    version             BIGINT      NOT NULL DEFAULT 0,
     */

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

  // Keep columnDefinition to match DB, map to String at JPA level
//   @Column(name = "slot", columnDefinition = "tstzrange", nullable = false)
//   private String slot;   /// huge problem here in this tstzrange 

  //  @Type(PostgreSQLSpringRangeType.class)
  @JdbcTypeCode(SqlTypes.OFFSET_DATE_TIME)
    @Column(
        name = "slot",
        columnDefinition = "tstzrange"
    )
    private org.springframework.data.domain.Range<OffsetDateTime> slot;


    @Column(name = "kind", nullable = false, length = 20)
    private String kind;


    // need to map the maintenance plan
    @Column(name = "maintenance_plan_id")
    private Long maintenancePlanId;

   
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "breakdown_request_id")
    private BreakdownRequest breakdownRequest;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "version", nullable = false)
    private Long version;

    @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY, optional = false)
    private WorkOrder workOrder;

    //getters and setters

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

    public org.springframework.data.domain.Range<OffsetDateTime> getSlot() {
        return slot;
    }

    public void setSlot(org.springframework.data.domain.Range<OffsetDateTime> slot) {
        this.slot = slot;
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

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }
}
