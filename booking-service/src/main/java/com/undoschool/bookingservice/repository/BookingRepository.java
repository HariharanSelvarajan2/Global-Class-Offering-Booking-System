package com.undoschool.bookingservice.repository;

import com.undoschool.bookingservice.entity.Booking;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Optional<Booking> findByParentIdAndOfferingId(UUID parentId, UUID offeringId);

    List<Booking> findByParentIdOrderByBookedAtDesc(UUID parentId);
}
