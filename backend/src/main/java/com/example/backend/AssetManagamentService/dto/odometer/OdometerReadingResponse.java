package com.example.backend.AssetManagamentService.dto.odometer;

import com.example.backend.AssetManagamentService.source.OdometerSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;

public class OdometerReadingResponse {

    private Long id;

    private BigDecimal readingKm;

    private OffsetDateTime readAt;

    private OdometerSource source;

    private Long recordedById;

    // getters setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getReadingKm() {
        return readingKm;
    }

    public void setReadingKm(BigDecimal readingKm) {
        this.readingKm = readingKm;
    }

    public OffsetDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(OffsetDateTime readAt) {
        this.readAt = readAt;
    }

    public OdometerSource getSource() {
        return source;
    }

    public void setSource(OdometerSource source) {
        this.source = source;
    }

    public Long getRecordedById() {
        return recordedById;
    }

    public void setRecordedById(Long recordedById) {
        this.recordedById = recordedById;
    }
}