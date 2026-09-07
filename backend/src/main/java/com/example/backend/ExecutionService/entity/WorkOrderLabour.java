package com.example.backend.ExecutionService.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "work_order_labour")
public class WorkOrderLabour {
    /*
     id             BIGINT GENERATED ALWAYS AS IDENTITY,
    work_order_id  BIGINT        NOT NULL,
    technician_id  BIGINT        NOT NULL,
    hours          NUMERIC(5,2)  NOT NULL,
    rate_applied   NUMERIC(12,2) NOT NULL,
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @ManyToOne
   @JoinColumn(name = "work_order_id", nullable = false)
   private WorkOrder workOrder;

   @Column(name = "technician_id", nullable = false)
   private Long technicianId;

   @Column(name = "hours", nullable = false, precision = 5, scale = 2)
   private BigDecimal hours;

   @Column(name = "rate_applied", nullable = false, precision = 12, scale = 2)
   private BigDecimal rateApplied;

   // getters and setters

   public Long getId() {
       return id;
   }

   public void setId(Long id) {
       this.id = id;
   }

    public WorkOrder getWorkOrder() {
         return workOrder;
    }

    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

   public Long getTechnicianId() {
       return technicianId;
   }

   public void setTechnicianId(Long technicianId) {
       this.technicianId = technicianId;
   }

   public BigDecimal getHours() {
       return hours;
   }

   public void setHours(BigDecimal hours) {
       this.hours = hours;
   }

   public BigDecimal getRateApplied() {
       return rateApplied;
   }

   public void setRateApplied(BigDecimal rateApplied) {
       this.rateApplied = rateApplied;
   }
}
