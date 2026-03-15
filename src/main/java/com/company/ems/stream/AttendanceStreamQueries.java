package com.company.ems.stream;

import com.company.ems.model.Attendance;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Các truy vấn in-memory (Stream API) cho Attendance.
 */
public final class AttendanceStreamQueries {

    private AttendanceStreamQueries() {}

    /**
     * Nhóm danh sách điểm danh theo ID của lớp học, 
     * sử dụng LinkedHashMap để giữ nguyên thứ tự.
     */
    public static Map<Long, List<Attendance>> groupByClassId(List<Attendance> attendances) {
        return attendances.stream()
                .filter(a -> a.getClazz() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getClazz().getClassId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    /**
     * Đếm tổng số buổi có trạng thái "Có mặt".
     */
    public static long countPresent(List<Attendance> attendances) {
        return attendances.stream()
                .filter(a -> "Có mặt".equals(a.getStatus()))
                .count();
    }

    /**
     * Đếm tổng số buổi có trạng thái "Vắng".
     */
    public static long countAbsent(List<Attendance> attendances) {
        return attendances.stream()
                .filter(a -> "Vắng".equals(a.getStatus()) || "Vắng mặt".equals(a.getStatus()))
                .count();
    }

    /**
     * Đếm tổng số buổi có trạng thái "Đi trễ".
     */
    public static long countLate(List<Attendance> attendances) {
        return attendances.stream()
                .filter(a -> "Đi trễ".equals(a.getStatus()))
                .count();
    }

    /**
     * Lọc ra các điểm danh của những lớp đã thanh toán dựa vào classIds.
     */
    public static List<Attendance> filterByPaidClasses(List<Attendance> allAttendances, java.util.Set<Long> paidClassIds) {
        return allAttendances.stream()
                .filter(a -> a.getClazz() != null && paidClassIds.contains(a.getClazz().getClassId()))
                .toList();
    }

    /**
     * Sắp xếp danh sách điểm danh theo ngày giảm dần.
     */
    public static java.util.stream.Stream<Attendance> sortByDateDesc(List<Attendance> attendances) {
         return attendances.stream()
                .sorted(Comparator.comparing(Attendance::getAttendDate).reversed());
    }

}
