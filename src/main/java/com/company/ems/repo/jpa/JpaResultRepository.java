package com.company.ems.repo.jpa;

import com.company.ems.model.Result;
import com.company.ems.repo.ResultRepository;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        // 1. Fetch student's results
        List<Result> studentResults = findByStudentId(em, studentId);
        if (studentResults.isEmpty()) return List.of();
        
        // 2. Identify the classes
        List<Long> classIds = studentResults.stream()
                .map(r -> r.getClazz().getClassId())
                .distinct()
                .toList();
                
        // 3. Fetch all scores for these classes to compute ranks
        List<Object[]> classScores = em.createQuery(
                "SELECT r.clazz.classId, r.score FROM Result r WHERE r.clazz.classId IN :cids AND r.score IS NOT NULL", Object[].class)
                .setParameter("cids", classIds)
                .getResultList();
                
        // Map classId -> list of scores
        Map<Long, List<BigDecimal>> scoresByClass = classScores.stream()
                .collect(Collectors.groupingBy(
                    row -> (Long) row[0],
                    Collectors.mapping(row -> (BigDecimal) row[1], Collectors.toList())
                ));
                
        // Sort scores descending
        scoresByClass.values().forEach(list -> list.sort(Comparator.reverseOrder()));
        
        // 4. Transform to RankedResult
        return studentResults.stream().map(res -> {
            if (res.getScore() == null) {
                return new com.company.ems.service.ResultService.RankedResult(res, 0, 0);
            }
            Long cid = res.getClazz().getClassId();
            List<BigDecimal> scores = scoresByClass.getOrDefault(cid, List.of());
            
            // indexOf gets the first matching element's index.
            // +1 gives us standard SQL RANK() behavior (1, 1, 3).
            int rank = scores.indexOf(res.getScore()) + 1;
            int total = scores.size();
            
            return new com.company.ems.service.ResultService.RankedResult(res, rank, total);
        }).toList();
    }
}

