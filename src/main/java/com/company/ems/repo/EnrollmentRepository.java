package com.company.ems.repo;

import com.company.ems.model.Enrollment;
import jakarta.persistence.EntityManager;

import java.util.List;

public interface EnrollmentRepository extends BaseRepository<Enrollment, Long> {
    List<Enrollment> findByStudentIdAndStatus(EntityManager em, Long studentId, String status);

    List<Enrollment> findByClassIdAndStatus(EntityManager em, Long classId, String status);

    List<Enrollment> findByStudentId(EntityManager em, Long studentId);
}