package com.undoschool.bookingservice.repository;

import com.undoschool.bookingservice.entity.ParentBookingLock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParentBookingLockRepository extends JpaRepository<ParentBookingLock, UUID> {

    @Modifying
    @Query(value = "insert into parent_booking_locks(parent_id) values (:parentId) on conflict do nothing", nativeQuery = true)
    void ensureExists(@Param("parentId") UUID parentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from ParentBookingLock l where l.parentId = :parentId")
    Optional<ParentBookingLock> findLocked(@Param("parentId") UUID parentId);
}
