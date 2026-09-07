package com.example.backend.InventoryService.entity;

import com.example.backend.CapacityAndSchedulingService.entity.Workshop;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table( name = "part_reorder_level")
public class PartReorderLevel {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "part_id",
            nullable = false
    )
    private Part part;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "workshop_id",
            nullable = false
    )
    private Workshop workshop;

    @Column(
            name = "reorder_level",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal reorderLevel;

    @Version
    @Column(nullable = false)
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Part getPart() {
        return part;
    }

    public void setPart(Part part) {
        this.part = part;
    }

    public Workshop getWorkshop() {
        return workshop;
    }

    public void setWorkshop(Workshop workshop) {
        this.workshop = workshop;
    }

    public BigDecimal getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(BigDecimal reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}