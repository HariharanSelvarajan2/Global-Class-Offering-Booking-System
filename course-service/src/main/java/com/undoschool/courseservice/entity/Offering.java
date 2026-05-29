package com.undoschool.courseservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "offerings")
public class Offering {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private UUID teacherId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 80)
    private String teacherTimezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferingStatus status = OfferingStatus.DRAFT;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Offering() {
    }

    public Offering(Course course, UUID teacherId, String name, String teacherTimezone) {
        this.course = course;
        this.teacherId = teacherId;
        this.name = name;
        this.teacherTimezone = teacherTimezone;
    }

    public UUID getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public UUID getTeacherId() {
        return teacherId;
    }

    public String getName() {
        return name;
    }

    public String getTeacherTimezone() {
        return teacherTimezone;
    }

    public OfferingStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void publish() {
        this.status = OfferingStatus.PUBLISHED;
    }
}
