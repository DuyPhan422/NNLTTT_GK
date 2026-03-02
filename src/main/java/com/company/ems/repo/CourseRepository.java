package com.company.ems.repo;

import com.company.ems.model.Course;
import jakarta.persistence.EntityManager;

import java.util.List;

public interface CourseRepository extends BaseRepository<Course, Long> {
    List<Course> findByStatus(EntityManager em, String status);
    List<Course> findByLevel(EntityManager em, String level);
    List<Course> findByNameContaining(EntityManager em, String keyword);
}

