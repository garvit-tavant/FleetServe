package com.example.backend.ExecutionService.entity;

import java.time.Instant;
import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "booking_history")
public class BookingHistory {
    /*
     id               BIGINT GENERATED ALWAYS AS IDENTITY,
    booking_id       BIGINT       NOT NULL,
    action           VARCHAR(50)  NOT NULL,
    previous_slot    TSTZRANGE,
    new_slot         TSTZRANGE,
    actor_id         BIGINT       NOT NULL,
    reason           VARCHAR(1000),
    occurred_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "action", nullable = false)
    private String action;

    @JdbcTypeCode(SqlTypes.OFFSET_DATE_TIME)
    @Column(name = "previous_slot",columnDefinition = "tstzrange")
    private org.springframework.data.domain.Range<OffsetDateTime> previousSlot;

    @JdbcTypeCode(SqlTypes.OFFSET_DATE_TIME)
    @Column(name = "new_slot",columnDefinition = "tstzrange")
    private org.springframework.data.domain.Range<OffsetDateTime> newSlot;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "reason")
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;


    // getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public org.springframework.data.domain.Range<OffsetDateTime> getPreviousSlot() {
        return previousSlot;
    }

    public void setPreviousSlot(org.springframework.data.domain.Range<OffsetDateTime> previousSlot) {
        this.previousSlot = previousSlot;
    }

    public org.springframework.data.domain.Range<OffsetDateTime> getNewSlot() {
        return newSlot;
    }

    public void setNewSlot(org.springframework.data.domain.Range<OffsetDateTime> newSlot) {
        this.newSlot = newSlot;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
