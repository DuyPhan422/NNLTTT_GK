package com.company.ems.repo.jpa;

import com.company.ems.model.Result;
import com.company.ems.repo.ResultRepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class JpaResultRepository extends JpaBaseRepository<Result, Long>
        implements ResultRepository {

    public JpaResultRepository() {
        super(Result.class);
    }

    @Override
    public List<Result> findByClassId(EntityManager em, Long classId) {
        return em.createQuery(
                "SELECT r FROM Result r " +
                "JOIN FETCH r.student s " +
                "JOIN FETCH r.clazz c " +
                "WHERE c.classId = :classId " +
                "ORDER BY s.fullName", Result.class)
                .setParameter("classId", classId)
                .getResultList();
    }

    @Override
    public List<Result> findByStudentAndClass(EntityManager em, Long studentId, Long classId) {
        return em.createQuery(
                "SELECT r FROM Result r " +
                "JOIN FETCH r.student " +
                "WHERE r.student.studentId = :sid AND r.clazz.classId = :cid",
                Result.class)
                .setParameter("sid", studentId)
                .setParameter("cid", classId)
                .getResultList();
    }

    @Override
    public List<Result> findByStudentId(EntityManager em, Long studentId) {
        return em.createQuery(
                "SELECT r FROM Result r " +
                "JOIN FETCH r.clazz c " +
                "LEFT JOIN FETCH c.course " +
                "WHERE r.student.studentId = :sid " +
                "ORDER BY r.createdAt DESC", Result.class)
                .setParameter("sid", studentId)
                .getResultList();
    }
}

