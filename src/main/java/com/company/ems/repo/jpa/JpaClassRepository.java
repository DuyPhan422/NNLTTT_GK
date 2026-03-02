package com.company.ems.repo.jpa;

import com.company.ems.model.Class;
import com.company.ems.repo.ClassRepository;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

public class JpaClassRepository extends JpaBaseRepository<Class, Long> implements ClassRepository {

    public JpaClassRepository() {
        super(Class.class);
    }

    @Override
    public List<Class> findByStatus(EntityManager em, String status) {
        return em.createQuery(
                        "SELECT c FROM Class c WHERE c.status = :status ORDER BY c.startDate", Class.class)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public List<Class> findByCourseId(EntityManager em, Long courseId) {
        return em.createQuery(
                        "SELECT c FROM Class c WHERE c.course.courseId = :courseId ORDER BY c.startDate", Class.class)
                .setParameter("courseId", courseId)
                .getResultList();
    }

    @Override
    public List<Class> findByTeacherId(EntityManager em, Long teacherId) {
        return em.createQuery(
                        "SELECT c FROM Class c WHERE c.teacher.teacherId = :teacherId ORDER BY c.startDate", Class.class)
                .setParameter("teacherId", teacherId)
                .getResultList();
    }

    @Override
    public List<Class> findStartingFrom(EntityManager em, LocalDate fromDate) {
        return em.createQuery(
                        "SELECT c FROM Class c WHERE c.startDate >= :fromDate ORDER BY c.startDate", Class.class)
                .setParameter("fromDate", fromDate)
                .getResultList();
    }
}

