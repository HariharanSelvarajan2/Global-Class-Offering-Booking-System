package com.undoschool.courseservice.repository;

import com.undoschool.courseservice.entity.Offering;
import com.undoschool.courseservice.entity.OfferingStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfferingRepository extends JpaRepository<Offering, UUID> {

    @EntityGraph(attributePaths = "course")
    List<Offering> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId);

    @Query("""
            select o from Offering o
            join fetch o.course
            where o.status = :status
              and exists (
                  select 1 from CourseSession s
                  where s.offering = o
              )
            order by o.createdAt desc
            """)
    List<Offering> findAvailable(@Param("status") OfferingStatus status);
}
