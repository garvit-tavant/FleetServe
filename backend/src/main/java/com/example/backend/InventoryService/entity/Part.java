package com.example.backend.InventoryService.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name="part")
public class Part {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
            name = "part_number",
            nullable = false,
            length = 100
    )
    private String partNumber;

    @Column(
            nullable = false,
            length = 500
    )
    private String description;

    @Column(
            name = "unit_of_measure",
            nullable = false,
            length = 30
    )
    private String unitOfMeasure;

    @Column(
            name = "standard_cost",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal standardCost;

    @Column(
            name = "is_active",
            nullable = false
    )
    private Boolean active = true;

    @Version
    @Column(
            nullable = false
    )
    private Long version;

    // getters setters


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getStandardCost() {
        return standardCost;
    }

    public void setStandardCost(BigDecimal standardCost) {
        this.standardCost = standardCost;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}