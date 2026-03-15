package com.company.ems.stream;

import com.company.ems.model.Result;
import com.company.ems.service.ResultService.RankedResult;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Các truy vấn in-memory (Stream API) cho Result.
 */
public final class ResultStreamQueries {

    private ResultStreamQueries() {}

    /** KPI thống kê kết quả học tập. */
    public record ResultKpi(long totalGraded, OptionalDouble avg, long passCount, long failCount) {}

    /**
     * Tính KPI từ danh sách result đã lọc.
     */
    public static ResultKpi buildKpi(List<Result> results) {
        long totalGraded = results.stream().filter(r -> r.getScore() != null).count();
        OptionalDouble avg = results.stream()
                .filter(r -> r.getScore() != null)
                .mapToDouble(r -> r.getScore().doubleValue()).average();
        long passCount = results.stream()
                .filter(r -> r.getScore() != null && r.getScore().doubleValue() >= 5.0).count();
        long failCount = results.stream()
                .filter(r -> r.getScore() != null && r.getScore().doubleValue() < 5.0).count();
        return new ResultKpi(totalGraded, avg, passCount, failCount);
    }

    /**
     * Lọc và sắp xếp kết quả theo lớp và từ khóa tìm kiếm.
     */
    public static List<Result> filterByClassAndKeyword(List<Result> results, Long classId, String keyword) {
        return results.stream()
                .filter(r -> classId == null
                        || (r.getClazz() != null && r.getClazz().getClassId().equals(classId)))
                .filter(r -> {
                    if (keyword == null || keyword.isEmpty()) return true;
                    String kw = keyword.toLowerCase();
                    String name = r.getStudent() != null ? r.getStudent().getFullName().toLowerCase() : "";
                    String code = r.getStudent() != null && r.getStudent().getStudentId() != null
                            ? ("hv" + String.format("%04d", r.getStudent().getStudentId())) : "";
                    return name.contains(kw) || code.contains(kw);
                })
                .sorted(Comparator.comparing(
                        r -> r.getStudent() != null ? r.getStudent().getFullName() : "",
                        String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    /**
     * Lọc danh sách RankedResult theo các lớp đã thanh toán.
     */
    public static List<RankedResult> filterByPaidClasses(List<RankedResult> results, Set<Long> paidClassIds) {
        return results.stream()
                .filter(rr -> rr.result().getClazz() != null
                           && paidClassIds.contains(rr.result().getClazz().getClassId()))
                .toList();
    }

    /**
     * Đếm số lớp đã có điểm tổng.
     */
    public static long countScored(List<RankedResult> results) {
        return results.stream()
                .filter(rr -> rr.result().getScore() != null)
                .count();
    }

    /**
     * Tính GPA trung bình của các môn đã có điểm tổng.
     */
    public static double calculateGpa(List<RankedResult> results) {
        return results.stream()
                .filter(rr -> rr.result().getScore() != null)
                .mapToDouble(rr -> rr.result().getScore().doubleValue())
                .average()
                .orElse(0.0);
    }

    /**
     * Đếm số lớp đạt (GPA >= 5.0).
     */
    public static long countPass(List<RankedResult> results) {
        return results.stream()
                .filter(rr -> rr.result().getScore() != null
                           && rr.result().getScore().doubleValue() >= 5.0)
                .count();
    }
}
