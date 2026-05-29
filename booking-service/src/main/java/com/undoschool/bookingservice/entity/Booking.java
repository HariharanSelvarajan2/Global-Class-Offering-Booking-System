package com.undoschool.bookingservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID parentId;

    @Column(nullable = false)
    private UUID offeringId;

    @Column(nullable = false, length = 160)
    private String courseName;

    @Column(nullable = false, length = 160)
    private String offeringName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(nullable = false)
    private Instant bookedAt = Instant.now();

    protected Booking() {
    }

    public Booking(UUID parentId, UUID offeringId, String courseName, String offeringName) {
        this.parentId = parentId;
        this.offeringId = offeringId;
        this.courseName = courseName;
        this.offeringName = offeringName;
    }

    public UUID getId() {
        return id;
    }

    public UUID getParentId() {
        return parentId;
    }

    public UUID getOfferingId() {
        return offeringId;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getOfferingName() {
        return offeringName;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getBookedAt() {
        return bookedAt;
    }
}
