package com.example.backend.CapacityAndSchedulingService.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "bay_capability")
public class BayCapability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "bay_id",
            nullable = false
    )
    private Bay bay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "capability_code",
            nullable = false
    )
    private Capability capability;

    public BayCapability() {
    }

    public Long getId() {
        return id;
    }

    public Bay getBay() {
        return bay;
    }

    public void setBay(Bay bay) {
        this.bay = bay;
    }

    public Capability getCapability() {
        return capability;
    }

    public void setCapability(
            Capability capability
    ) {
        this.capability = capability;
    }
}