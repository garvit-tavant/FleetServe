package com.example.backend.AssetManagamentService.entity;

import com.example.backend.AssetManagamentService.source.OdometerSource;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "odometer_reading")
public class OdometerReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(
            name = "reading_km",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal readingKm;

    @Column(name = "read_at", nullable = false)
    private OffsetDateTime readAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OdometerSource source;

    @Column(name = "recorded_by_id", nullable = false)
    private Long recordedById;

    public OdometerReading() {
    }

    public Long getId() {
        return id;
    }

    public Asset getAsset() {
        return asset;
    }

    public BigDecimal getReadingKm() {
        return readingKm;
    }

    public OffsetDateTime getReadAt() {
        return readAt;
    }

    public OdometerSource getSource() {
        return source;
    }

    public Long getRecordedById() {
        return recordedById;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public void setReadingKm(BigDecimal readingKm) {
        this.readingKm = readingKm;
    }

    public void setReadAt(OffsetDateTime readAt) {
        this.readAt = readAt;
    }

    public void setSource(OdometerSource source) {
        this.source = source;
    }

    public void setRecordedById(Long recordedById) {
        this.recordedById = recordedById;
    }
}