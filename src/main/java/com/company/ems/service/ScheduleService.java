package com.company.ems.service;

import com.company.ems.model.Schedule;
import com.company.ems.repo.ScheduleRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ScheduleService extends AbstractBaseService<Schedule, Long> {

    private final ScheduleRepository scheduleRepository;

    public ScheduleService(ScheduleRepository scheduleRepository) {
        super(scheduleRepository);
        this.scheduleRepository = scheduleRepository;
    }

    public List<Schedule> findByClassId(Long classId) {
        try {
            return txManager.runInTransaction(em -> scheduleRepository.findByClassId(em, classId));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải lịch học của lớp: " + e.getMessage(), e);
        }
    }

    public List<Schedule> findByDateRange(LocalDate from, LocalDate to) {
        try {
            return txManager.runInTransaction(em -> scheduleRepository.findByDateRange(em, from, to));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải lịch học theo khoảng ngày: " + e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra xung đột phòng học.
     * @return danh sách lịch xung đột (rỗng = không trùng)
     */
    public List<Schedule> checkRoomConflicts(Long roomId,
                                              LocalDate date,
                                              LocalTime startTime,
                                              LocalTime endTime,
                                              Long excludeScheduleId) {
        try {
            return txManager.runInTransaction(em ->
                    scheduleRepository.findRoomConflicts(em, roomId, date, startTime, endTime, excludeScheduleId));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi kiểm tra xung đột phòng: " + e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra xung đột giáo viên.
     * @return danh sách lịch xung đột (rỗng = không trùng)
     */
    public List<Schedule> checkTeacherConflicts(Long teacherId,
                                                 LocalDate date,
                                                 LocalTime startTime,
                                                 LocalTime endTime,
                                                 Long excludeScheduleId) {
        try {
            return txManager.runInTransaction(em ->
                    scheduleRepository.findTeacherConflicts(em, teacherId, date, startTime, endTime, excludeScheduleId));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi kiểm tra xung đột giáo viên: " + e.getMessage(), e);
        }
    }
}

