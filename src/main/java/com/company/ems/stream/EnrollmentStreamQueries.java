package com.company.ems.stream;

import com.company.ems.model.Enrollment;
import com.company.ems.model.Student;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Các truy vấn dữ liệu stream dành cho Enrollment, tách biệt logic tính toán
 * khỏi UI.
 */
public final class EnrollmentStreamQueries {

        private EnrollmentStreamQueries() {
        }

        /**
         * DTO đại diện cho dữ liệu cơ bản của hóa đơn nợ Enrollment
         */
        public record EnrollmentView(
                        Long enrollmentId,
                        String courseName,
                        String className,
                        BigDecimal fee) {
        }

        // ======================================================================
        // 1) Tính tổng tiền học phí dựa trên khóa học class
        // ======================================================================
        public static BigDecimal calculateTotalFee(List<Enrollment> enrollments) {
                if (enrollments == null)
                        return BigDecimal.ZERO;
                return enrollments.stream()
                                .filter(e -> e.getClazz() != null && e.getClazz().getCourse() != null)
                                .map(e -> e.getClazz().getCourse().getFee())
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // ======================================================================
        // 2) Tách Set các Enrollment IDs
        // ======================================================================
        public static Set<Long> mapToEnrollmentIds(List<Enrollment> enrollments) {
                if (enrollments == null)
                        return Set.of();
                return enrollments.stream()
                                .map(Enrollment::getEnrollmentId)
                                .collect(Collectors.toSet());
        }

        // ======================================================================
        // 3) Nối danh sách các Enrollment IDs thành chuỗi phân cách
        // ======================================================================
        public static String joinEnrollmentIds(List<Enrollment> enrollments, String delimiter) {
                if (enrollments == null)
                        return "";
                return enrollments.stream()
                                .map(e -> String.valueOf(e.getEnrollmentId()))
                                .collect(Collectors.joining(delimiter));
        }

        // ======================================================================
        // 4) Nối danh sách Tên các Khóa học thành chuỗi phân cách
        // ======================================================================
        public static String joinCourseNames(List<Enrollment> enrollments, String delimiter) {
                if (enrollments == null)
                        return "";
                return enrollments.stream()
                                .filter(e -> e.getClazz() != null && e.getClazz().getCourse() != null)
                                .map(e -> e.getClazz().getCourse().getCourseName())
                                .collect(Collectors.joining(delimiter));
        }

        // ======================================================================
        // 5) Lọc ra các Enrollment còn lại chưa thanh toán dựa theo exclusion Set IDs
        // ======================================================================
        public static List<Enrollment> filterRemaining(List<Enrollment> enrollments, Set<Long> chosenIds) {
                if (enrollments == null)
                        return List.of();
                if (chosenIds == null || chosenIds.isEmpty())
                        return enrollments;
                return enrollments.stream()
                                .filter(e -> !chosenIds.contains(e.getEnrollmentId()))
                                .collect(Collectors.toList());
        }

        // ======================================================================
        // 6) Chuyển dữ liệu sang dạng DTO View
        // ======================================================================
        public static List<EnrollmentView> toEnrollmentViews(List<Enrollment> enrollments) {
                if (enrollments == null)
                        return List.of();
                return enrollments.stream()
                                .map(e -> {
                                        String courseName = (e.getClazz() != null && e.getClazz().getCourse() != null)
                                                        ? e.getClazz().getCourse().getCourseName()
                                                        : "(Không rõ)";
                                        String className = e.getClazz() != null ? e.getClazz().getClassName()
                                                        : "(Không rõ)";
                                        BigDecimal fee = (e.getClazz() != null && e.getClazz().getCourse() != null)
                                                        ? e.getClazz().getCourse().getFee()
                                                        : BigDecimal.ZERO;

                                        return new EnrollmentView(e.getEnrollmentId(), courseName, className, fee);
                                })
                                .collect(Collectors.toList());
        }
}
