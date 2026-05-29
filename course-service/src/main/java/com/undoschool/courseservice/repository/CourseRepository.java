package com.undoschool.courseservice.repository;

import com.undoschool.courseservice.entity.Course;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    Optional<Course> findByNameIgnoreCase(String name);
}
