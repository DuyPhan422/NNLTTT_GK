package com.company.ems.repo.jpa;

import com.company.ems.model.Enrollment;
import com.company.ems.repo.EnrollmentRepository;

import jakarta.persistence.EntityManager;
import java.util.List;

public class JpaEnrollmentRepository extends JpaBaseRepository<Enrollment, Long> implements EnrollmentRepository {

    public JpaEnrollmentRepository() {
        super(Enrollment.class);
    }

    @Override
    public List<Enrollment> findByStudentIdAndStatus(EntityManager em, Long studentId, String status) {
        return em.createQuery(
                "SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.status = :status", Enrollment.class)
                .setParameter("studentId", studentId)
                .setParameter("status", status)
                .getResultList();

    }

    @Override
    public List<Enrollment> findByStudentId(EntityManager em, Long studentId) {
        return em.createQuery(
                "SELECT e FROM Enrollment e WHERE e.student.id = :studentId", Enrollment.class)
                .setParameter("studentId", studentId)
                .getResultList();
    }

    @Override
    public List<Enrollment> findByClassIdAndStatus(EntityManager em, Long classId, String status) {
        return em.createQuery(
                "SELECT e FROM Enrollment e WHERE e.clazz.id = :classId AND e.status = :status", Enrollment.class)
                .setParameter("classId", classId)
                .setParameter("status", status)
                .getResultList();
    }
}