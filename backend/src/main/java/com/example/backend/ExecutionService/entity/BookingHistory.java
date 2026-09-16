package com.example.backend.ExecutionService.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Range;

import com.example.backend.SecurityService.entity.AppUser;

import jakarta.persistence.*;

@Entity
@Table(name = "booking_history")
public class BookingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false)
    @JoinColumn(
            name = "booking_id",
            nullable = false)
    private Booking booking;

    @Column(
            name = "action",
            nullable = false,
            length = 50)
    private String action;

    @Column(name = "previous_start_at")
    private OffsetDateTime previousStartAt;

    @Column(name = "previous_end_at")
    private OffsetDateTime previousEndAt;

    @Column(name = "new_start_at")
    private OffsetDateTime newStartAt;

    @Column(name = "new_end_at")
    private OffsetDateTime newEndAt;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false)
    @JoinColumn(
            name = "actor_id",
            nullable = false)
    private AppUser actor;

    @Column(
            name = "reason",
            length = 1000)
    private String reason;

    @Column(
            name = "occurred_at",
            nullable = false)
    private OffsetDateTime occurredAt;

    // getters setters


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public AppUser getActor() {
        return actor;
    }

    public void setActor(AppUser actor) {
        this.actor = actor;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public OffsetDateTime getPreviousStartAt() {
        return previousStartAt;
    }

    public void setPreviousStartAt(OffsetDateTime previousStartAt) {
        this.previousStartAt = previousStartAt;
    }

    public OffsetDateTime getPreviousEndAt() {
        return previousEndAt;
    }

    public void setPreviousEndAt(OffsetDateTime previousEndAt) {
        this.previousEndAt = previousEndAt;
    }

    public OffsetDateTime getNewStartAt() {
        return newStartAt;
    }

    public void setNewStartAt(OffsetDateTime newStartAt) {
        this.newStartAt = newStartAt;
    }

    public OffsetDateTime getNewEndAt() {
        return newEndAt;
    }

    public void setNewEndAt(OffsetDateTime newEndAt) {
        this.newEndAt = newEndAt;
    }
}