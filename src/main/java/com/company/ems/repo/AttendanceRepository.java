package com.company.ems.repo;

import com.company.ems.model.Attendance;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends BaseRepository<Attendance, Long> {

    /** Toàn bộ điểm danh của một lớp trong một ngày cụ thể */
    List<Attendance> findByClassAndDate(EntityManager em, Long classId, LocalDate date);

    /** Toàn bộ điểm danh của một lớp (tất cả ngày) */
    List<Attendance> findByClassId(EntityManager em, Long classId);

    /** Toàn bộ điểm danh của một học viên */
    List<Attendance> findByStudentId(EntityManager em, Long studentId);

    /** Điểm danh của một học viên trong một lớp */
    List<Attendance> findByStudentAndClass(EntityManager em, Long studentId, Long classId);

    /** Kiểm tra đã điểm danh chưa (tránh duplicate) */
    boolean existsByStudentClassDate(EntityManager em, Long studentId, Long classId, LocalDate date);

    /** Đếm số buổi theo status của một học viên trong một lớp */
    long countByStudentClassStatus(EntityManager em, Long studentId, Long classId, String status);

    /** Thống kê: số buổi vắng theo từng lớp (dùng cho Admin dashboard) */
    List<Object[]> getAbsenceSummaryByClass(EntityManager em);

    /** Thống kê: tỉ lệ điểm danh toàn trung tâm trong khoảng ngày */
    List<Object[]> getAttendanceRateByClass(EntityManager em, LocalDate from, LocalDate to);

    /** Lấy các ngày đã có điểm danh của một lớp */
    List<LocalDate> findAttendanceDatesByClass(EntityManager em, Long classId);
}

