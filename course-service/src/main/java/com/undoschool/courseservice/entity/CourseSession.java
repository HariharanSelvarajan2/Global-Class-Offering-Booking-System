package com.undoschool.courseservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_sessions")
public class CourseSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offering_id", nullable = false)
    private Offering offering;

    @Column(nullable = false)
    private UUID teacherId;

    @Column(nullable = false)
    private Instant startAt;

    @Column(nullable = false)
    private Instant endAt;

    @Column(nullable = false, length = 80)
    private String sourceTimezone;

    protected CourseSession() {
    }

    public CourseSession(Offering offering, UUID teacherId, Instant startAt, Instant endAt, String sourceTimezone) {
        this.offering = offering;
        this.teacherId = teacherId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.sourceTimezone = sourceTimezone;
    }

    public UUID getId() {
        return id;
    }

    public Offering getOffering() {
        return offering;
    }

    public UUID getTeacherId() {
        return teacherId;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public String getSourceTimezone() {
        return sourceTimezone;
    }
}
