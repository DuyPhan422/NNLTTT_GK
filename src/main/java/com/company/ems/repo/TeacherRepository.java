package com.company.ems.repo;

import com.company.ems.model.Teacher;
import jakarta.persistence.EntityManager;

import java.util.List;

public interface TeacherRepository extends BaseRepository<Teacher, Long> {
    List<Teacher> findByStatus(EntityManager em, String status);
    List<Teacher> findBySpecialty(EntityManager em, String specialty);
}

