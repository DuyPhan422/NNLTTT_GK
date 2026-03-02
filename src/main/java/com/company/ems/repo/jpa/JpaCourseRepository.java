package com.company.ems.repo.jpa;

import com.company.ems.model.Course;
import com.company.ems.repo.CourseRepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class JpaCourseRepository extends JpaBaseRepository<Course, Long> implements CourseRepository {

    public JpaCourseRepository() {
        super(Course.class);
    }

    @Override
    public List<Course> findByStatus(EntityManager em, String status) {
        return em.createQuery(
                "SELECT c FROM Course c WHERE c.status = :status ORDER BY c.courseName", Course.class)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public List<Course> findByLevel(EntityManager em, String level) {
        return em.createQuery(
                "SELECT c FROM Course c WHERE c.level = :level ORDER BY c.courseName", Course.class)
                .setParameter("level", level)
                .getResultList();
    }

    @Override
    public List<Course> findByNameContaining(EntityManager em, String keyword) {
        return em.createQuery(
                "SELECT c FROM Course c WHERE LOWER(c.courseName) LIKE LOWER(:kw) ORDER BY c.courseName", Course.class)
                .setParameter("kw", "%" + keyword + "%")
                .getResultList();
    }
}

