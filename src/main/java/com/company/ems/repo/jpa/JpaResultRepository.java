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

    /** Override để JOIN FETCH student, clazz và course — tránh LazyInitializationException. */
    @Override
    public List<Result> findAll(EntityManager em) {
        return em.createQuery(
                "SELECT r FROM Result r " +
                "JOIN FETCH r.student s " +
                "LEFT JOIN FETCH r.clazz c " +
                "LEFT JOIN FETCH c.course " +
                "ORDER BY s.fullName", Result.class)
                .getResultList();
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

    @Override
    public List<com.company.ems.service.ResultService.RankedResult> findRankedResultsByStudentId(EntityManager em, Long studentId) {
        // Native query to calculate rank for a student's results
        String nativeQuery = """
            WITH ClassRank AS (
                SELECT
                    r.id,
                    r.class_id,
                    r.score,
                    RANK() OVER(PARTITION BY r.class_id ORDER BY r.score DESC) as rnk,
                    COUNT(*) OVER(PARTITION BY r.class_id) as total
                FROM results r
                WHERE r.score IS NOT NULL
            )
            SELECT
                r.*,
                COALESCE(cr.rnk, 0) as student_rank,
                COALESCE(cr.total, 0) as total_in_class
            FROM results r
            LEFT JOIN ClassRank cr ON r.id = cr.id
            WHERE r.student_id = :sid
        """;
        
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(nativeQuery, Result.class)
            .setParameter("sid", studentId)
            .getResultList();
            
        return rows.stream().map(row -> {
            Result res = (Result) row[0];
            int rank = ((Number) row[1]).intValue();
            int total = ((Number) row[2]).intValue();
            return new com.company.ems.service.ResultService.RankedResult(res, rank, total);
        }).toList();
    }
}

