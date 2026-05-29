package com.undoschool.courseservice.repository;

import com.undoschool.courseservice.entity.CourseSession;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseSessionRepository extends JpaRepository<CourseSession, UUID> {
    List<CourseSession> findByOfferingIdOrderByStartAt(UUID offeringId);

    List<CourseSession> findByOfferingIdAndEndAtGreaterThanEqualOrderByStartAt(UUID offeringId, Instant fromTime);
}
