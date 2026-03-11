package com.company.ems.service;

import com.company.ems.model.Class;
import com.company.ems.model.Result;
import com.company.ems.model.Student;
import com.company.ems.repo.ResultRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResultService extends AbstractBaseService<Result, Long> {

    private final ResultRepository resultRepository;

    public ResultService(ResultRepository resultRepository) {
        super(resultRepository);
        this.resultRepository = resultRepository;
    }

    public List<Result> findByClassId(Long classId) {
        try {
            return txManager.runInTransaction(em -> resultRepository.findByClassId(em, classId));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải kết quả lớp: " + e.getMessage(), e);
        }
    }

    public List<Result> findByStudentId(Long studentId) {
        try {
            return txManager.runInTransaction(em -> resultRepository.findByStudentId(em, studentId));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải kết quả học viên: " + e.getMessage(), e);
        }
    }

    /**
     * Chuẩn bị sheet nhập điểm cho một lớp:
     * - Nếu học viên đã có Result → trả về record hiện tại (để chỉnh sửa)
     * - Nếu chưa có → tạo Result mới (transient, chưa lưu DB)
     * Dùng Stream để map student list → result list (upsert pattern giống AttendanceService).
     */
    public List<Result> prepareResultSheet(Class clazz, List<Student> enrolledStudents) {
        try {
            List<Result> sheet = txManager.runInTransaction(em -> {
                List<Result> existing = resultRepository.findByClassId(em, clazz.getClassId());

                // Map studentId → existing Result để lookup O(1)
                Map<Long, Result> existingMap = existing.stream()
                        .collect(Collectors.toMap(
                                r -> r.getStudent().getStudentId(),
                                r -> r));

                // Stream: với mỗi học viên enrolled, lấy existing hoặc tạo mới
                List<Result> result = enrolledStudents.stream()
                        .map(student -> {
                            Result r = existingMap.get(student.getStudentId());
                            return r != null ? r : buildNewResult(clazz, student);
                        })
                        .collect(Collectors.toList());
                return result;
            });
            return sheet;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi chuẩn bị sheet điểm: " + e.getMessage(), e);
        }
    }

    /**
     * Lưu hàng loạt kết quả trong một transaction (upsert via merge).
     */
    public void saveAll(List<Result> results) {
        try {
            txManager.runInTransaction(em -> {
                results.forEach(em::merge);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lưu kết quả: " + e.getMessage(), e);
        }
    }

    /**
     * Tính điểm tổng theo tỉ lệ 25% – 25% – 50%.
     * Nếu thiếu bất kỳ thành phần nào thì trả về null.
     */
    public static BigDecimal calcTotal(BigDecimal s1, BigDecimal s2, BigDecimal sf) {
        if (s1 == null || s2 == null || sf == null) return null;
        return s1.multiply(new BigDecimal("0.25"))
                .add(s2.multiply(new BigDecimal("0.25")))
                .add(sf.multiply(new BigDecimal("0.50")))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Tự động xếp loại dựa trên điểm tổng.
     * Thang điểm 10:
     *   A+: ≥9.0 | A: ≥8.5 | A-: ≥8.0 | B+: ≥7.0 | B: ≥6.5 | B-: ≥6.0 | C: ≥5.0 | D: ≥4.0 | F: <4.0
     */
    public static String autoGrade(double score) {
        record Threshold(double min, String grade) {}
        return java.util.stream.Stream.of(
                new Threshold(9.0, "A+"),
                new Threshold(8.5, "A"),
                new Threshold(8.0, "A-"),
                new Threshold(7.0, "B+"),
                new Threshold(6.5, "B"),
                new Threshold(6.0, "B-"),
                new Threshold(5.0, "C"),
                new Threshold(4.0, "D"),
                new Threshold(0.0, "F"))
                .filter(t -> score >= t.min())
                .findFirst()
                .map(Threshold::grade)
                .orElse("F");
    }

    /**
     * Lấy kết quả học tập của học viên, kèm xếp hạng trong lớp.
     * Mỗi Result sẽ được annotate rank trong field comment tạm thời (không lưu DB),
     * thực ra rank được tính và trả về qua RankedResult record.
     */
    public List<RankedResult> findByStudentIdWithRanking(Long studentId) {
        try {
            List<Result> results = findByStudentId(studentId);
            // Với mỗi result, lấy toàn bộ kết quả của lớp đó để tính hạng
            return results.stream().map(r -> {
                int rank = 0, total = 0;
                try {
                    List<Result> classResults = findByClassId(r.getClazz().getClassId());
                    // Chỉ tính hạng với những học viên có điểm tổng
                    List<Result> withScore = classResults.stream()
                            .filter(cr -> cr.getScore() != null)
                            .sorted(Comparator.comparing(Result::getScore).reversed())
                            .toList();
                    total = withScore.size();
                    if (r.getScore() != null) {
                        rank = 1;
                        for (Result cr : withScore) {
                            if (cr.getScore().compareTo(r.getScore()) > 0) rank++;
                        }
                    }
                } catch (Exception ignored) {}
                return new RankedResult(r, rank, total);
            }).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải kết quả học tập: " + e.getMessage(), e);
        }
    }

    /** DTO: kết quả + xếp hạng trong lớp */
    public record RankedResult(Result result, int rank, int totalInClass) {}

    // ── Private helpers ───────────────────────────────────────────────────

    private Result buildNewResult(Class clazz, Student student) {
        Result r = new Result();
        r.setClazz(clazz);
        r.setStudent(student);
        r.setScore1(null);
        r.setScore2(null);
        r.setFinalScore(null);
        r.setScore(null);
        r.setGrade(null);
        r.setComment(null);
        return r;
    }
}

