package com.example.backend.CapacityAndSchedulingService.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_bay")
public class Bay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "workshop_id",
            nullable = false
    )
    private Workshop workshop;

    @Column(
            name = "bay_code",
            nullable = false,
            length = 30
    )
    private String bayCode;

    @Column(
            name = "is_active",
            nullable = false
    )
    private Boolean isActive = true;

    @Version
    private Long version;

    @OneToMany(
            mappedBy = "bay",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<BayCapability> capabilities =
            new ArrayList<>();

    public Bay() {
    }

    public Long getId() {
        return id;
    }

    public Workshop getWorkshop() {
        return workshop;
    }

    public void setWorkshop(Workshop workshop) {
        this.workshop = workshop;
    }

    public String getBayCode() {
        return bayCode;
    }

    public void setBayCode(String bayCode) {
        this.bayCode = bayCode;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Long getVersion() {
        return version;
    }

    public List<BayCapability> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(
            List<BayCapability> capabilities
    ) {
        this.capabilities = capabilities;
    }
}