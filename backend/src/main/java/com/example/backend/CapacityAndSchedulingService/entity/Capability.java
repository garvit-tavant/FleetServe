package com.example.backend.CapacityAndSchedulingService.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "capability")
public class Capability {

    @Id
    @Column(
            name = "capability_code",
            nullable = false,
            length = 50
    )
    private String capabilityCode;

    @Column(
            nullable = false,
            length = 100
    )
    private String name;

    @Column(length = 500)
    private String description;

    @Version
    private Long version;

    @OneToMany(
            mappedBy = "capability",
            cascade = CascadeType.ALL
    )
    private List<BayCapability> bayCapabilities =
            new ArrayList<>();

    public Capability() {
    }

    public String getCapabilityCode() {
        return capabilityCode;
    }

    public void setCapabilityCode(
            String capabilityCode
    ) {
        this.capabilityCode = capabilityCode;
    }

    public String getName() {
        return name;
    }

    public void setName(
            String name
    ) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(
            String description
    ) {
        this.description = description;
    }
}