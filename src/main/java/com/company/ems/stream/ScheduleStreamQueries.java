package com.company.ems.stream;

import com.company.ems.model.Schedule;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Các truy vấn in-memory (Stream API) cho Schedule.
 */
public final class ScheduleStreamQueries {

    private ScheduleStreamQueries() {}

    /**
     * Lọc lịch học theo lớp được chọn (hoặc tất cả).
     */
    public static List<Schedule> filterByClassName(List<Schedule> schedules, String selectedClassName) {
        return schedules.stream()
                .filter(s -> selectedClassName == null
                        || "Tất cả lớp".equals(selectedClassName)
                        || s.getClazz().getClassName().equals(selectedClassName))
                .toList();
    }

    /**
     * Lọc lịch học theo danh sách các lớp của học viên.
     */
    public static List<Schedule> filterByMyClasses(List<Schedule> allSchedules, Set<Long> myClassIds) {
        return allSchedules.stream()
                .filter(s -> s.getClazz() != null && myClassIds.contains(s.getClazz().getClassId()))
                .toList();
    }

    /**
     * Lọc lịch học theo ngày và khung giờ (buổi học), sắp xếp theo giờ bắt đầu.
     */
    public static List<Schedule> filterBySession(List<Schedule> schedules, LocalDate day, int startHour, int endHour) {
        return schedules.stream()
                .filter(s -> s.getStudyDate().equals(day)
                        && s.getStartTime().getHour() >= startHour
                        && s.getStartTime().getHour() < endHour)
                .sorted(Comparator.comparing(Schedule::getStartTime))
                .toList();
    }
}
