package com.undoschool.courseservice.repository;

import com.undoschool.courseservice.entity.Offering;
import com.undoschool.courseservice.entity.OfferingStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OfferingRepository extends JpaRepository<Offering, UUID> {

    @Query("""
            select distinct o from Offering o
            join fetch o.course
            left join CourseSession s on s.offering = o
            where o.teacherId = :teacherId
              and s.endAt >= :fromTime
            order by o.createdAt desc
            """)
    List<Offering> findUpcomingByTeacherId(UUID teacherId, Instant fromTime);

    @Query("""
            select distinct o from Offering o
            join fetch o.course
            join CourseSession s on s.offering = o
            where o.status = :status
              and s.endAt >= :fromTime
            order by o.createdAt desc
            """)
    List<Offering> findAvailable(OfferingStatus status, Instant fromTime);
}
