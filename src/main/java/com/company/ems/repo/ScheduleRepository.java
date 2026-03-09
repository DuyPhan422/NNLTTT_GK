package com.company.ems.repo;

import com.company.ems.model.Schedule;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ScheduleRepository extends BaseRepository<Schedule, Long> {

    /** Toàn bộ lịch của một lớp, sắp theo ngày rồi giờ */
    List<Schedule> findByClassId(EntityManager em, Long classId);

    /** Lịch học trong khoảng ngày (dùng cho lịch tổng quan) */
    List<Schedule> findByDateRange(EntityManager em, LocalDate from, LocalDate to);

    /**
     * Kiểm tra trùng phòng học trong cùng ngày & khung giờ.
     * Trả về các bản ghi xung đột (loại trừ scheduleId hiện tại khi chỉnh sửa).
     */
    List<Schedule> findRoomConflicts(EntityManager em,
                                     Long roomId,
                                     LocalDate date,
                                     LocalTime startTime,
                                     LocalTime endTime,
                                     Long excludeScheduleId);

    /**
     * Kiểm tra trùng giáo viên trong cùng ngày & khung giờ.
     */
    List<Schedule> findTeacherConflicts(EntityManager em,
                                        Long teacherId,
                                        LocalDate date,
                                        LocalTime startTime,
                                        LocalTime endTime,
                                        Long excludeScheduleId);
}
