package com.company.ems.service;

import com.company.ems.model.Class;
import com.company.ems.model.Result;
import com.company.ems.model.Student;
import com.company.ems.repo.ResultRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResultService extends AbstractBaseService<Result, Long> {

    /** Kết quả kèm thứ hạng trong lớp. */
    public record RankedResult(Result result, int rank, int totalInClass) {}

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
     * Trả về danh sách kết quả của học viên kèm thứ hạng trong từng lớp.
     */
    public List<RankedResult> findByStudentIdWithRanking(Long studentId) {
        try {
            return txManager.runInTransaction(em -> {
                List<Result> myResults = resultRepository.findByStudentId(em, studentId);
                return myResults.stream().map(r -> {
                    if (r.getClazz() == null || r.getScore() == null) {
                        return new RankedResult(r, 0, 0);
                    }
                    List<Result> classResults = resultRepository.findByClassId(em, r.getClazz().getClassId());
                    List<Double> scores = classResults.stream()
                            .filter(cr -> cr.getScore() != null)
                            .map(cr -> cr.getScore().doubleValue())
                            .sorted(java.util.Comparator.reverseOrder())
                            .collect(java.util.stream.Collectors.toList());
                    int total = scores.size();
                    double myScore = r.getScore().doubleValue();
                    int rank = (int) scores.stream().filter(s -> s > myScore).count() + 1;
                    return new RankedResult(r, rank, total);
                }).collect(java.util.stream.Collectors.toList());
            });
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải bảng điểm học viên: " + e.getMessage(), e);
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
     * Tính điểm tổng từ 3 thành phần: QT1×25% + QT2×25% + Cuối kỳ×50%.
     * Trả về null nếu cả 3 đều null (chưa nhập điểm).
     * Nếu chỉ nhập một phần, các cột null được coi là 0.
     */
    public static java.math.BigDecimal calcTotal(java.math.BigDecimal score1,
                                                  java.math.BigDecimal score2,
                                                  java.math.BigDecimal finalScore) {
        if (score1 == null && score2 == null && finalScore == null) return null;
        java.math.BigDecimal s1 = score1     != null ? score1     : java.math.BigDecimal.ZERO;
        java.math.BigDecimal s2 = score2     != null ? score2     : java.math.BigDecimal.ZERO;
        java.math.BigDecimal sf = finalScore != null ? finalScore : java.math.BigDecimal.ZERO;
        return s1.multiply(new java.math.BigDecimal("0.25"))
                 .add(s2.multiply(new java.math.BigDecimal("0.25")))
                 .add(sf.multiply(new java.math.BigDecimal("0.50")))
                 .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Tự động xếp loại dựa trên điểm số.
     * Stream pipeline: danh sách ngưỡng → tìm cái phù hợp đầu tiên.
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

    // ── Private helpers ───────────────────────────────────────────────────

    private Result buildNewResult(Class clazz, Student student) {
        Result r = new Result();
        r.setClazz(clazz);
        r.setStudent(student);
        r.setScore(null);
        r.setGrade(null);
        r.setComment(null);
        return r;
    }
}

