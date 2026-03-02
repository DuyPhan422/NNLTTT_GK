package com.company.ems.repo.jpa;

import com.company.ems.model.Teacher;
import com.company.ems.repo.TeacherRepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class JpaTeacherRepository extends JpaBaseRepository<Teacher, Long> implements TeacherRepository {

    public JpaTeacherRepository() {
        super(Teacher.class);
    }

    @Override
    public List<Teacher> findByStatus(EntityManager em, String status) {
        return em.createQuery(
                "SELECT t FROM Teacher t WHERE t.status = :status ORDER BY t.fullName", Teacher.class)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public List<Teacher> findBySpecialty(EntityManager em, String specialty) {
        return em.createQuery(
                "SELECT t FROM Teacher t WHERE LOWER(t.specialty) LIKE LOWER(:sp) ORDER BY t.fullName", Teacher.class)
                .setParameter("sp", "%" + specialty + "%")
                .getResultList();
    }
}

