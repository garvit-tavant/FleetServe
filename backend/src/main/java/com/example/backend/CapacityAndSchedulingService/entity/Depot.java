package com.example.backend.CapacityAndSchedulingService.entity;

import com.example.backend.AssetManagamentService.entity.Asset;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "depot")
public class Depot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            length = 30
    )
    private String code;

    @Column(
            nullable = false,
            length = 100
    )
    private String region;

    @Column(
            name = "is_active",
            nullable = false
    )
    private Boolean isActive = true;

    @Version
    private Long version;

    @OneToMany(
            mappedBy = "depot"
    )
    private List<Workshop> workshops =
            new ArrayList<>();

    @OneToMany(
            mappedBy = "homeDepot"
    )
    private List<Asset> assets =
            new ArrayList<>();

    public Depot() {
    }

    // getters setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<Workshop> getWorkshops() {
        return workshops;
    }

    public void setWorkshops(List<Workshop> workshops) {
        this.workshops = workshops;
    }

    public List<Asset> getAssets() {
        return assets;
    }

    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }
}
