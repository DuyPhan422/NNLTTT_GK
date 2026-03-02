package com.company.ems.repo;

import com.company.ems.model.Class;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

public interface ClassRepository extends BaseRepository<Class, Long> {
    List<Class> findByStatus(EntityManager em, String status);
    List<Class> findByCourseId(EntityManager em, Long courseId);
    List<Class> findByTeacherId(EntityManager em, Long teacherId);
    List<Class> findStartingFrom(EntityManager em, LocalDate fromDate);
}

