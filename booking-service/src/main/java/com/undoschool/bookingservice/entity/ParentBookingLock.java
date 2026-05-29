package com.undoschool.bookingservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "parent_booking_locks")
public class ParentBookingLock {

    @Id
    private UUID parentId;

    protected ParentBookingLock() {
    }

    public ParentBookingLock(UUID parentId) {
        this.parentId = parentId;
    }

    public UUID getParentId() {
        return parentId;
    }
}
