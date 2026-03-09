package com.company.ems.service;

import com.company.ems.model.Attendance;
import com.company.ems.model.Student;
import com.company.ems.repo.AttendanceRepository;

import java.time.LocalDate;
import java.util.List;

public class AttendanceService extends AbstractBaseService<Attendance, Long> {

    private final AttendanceRepository attendanceRepository;

    public AttendanceService(AttendanceRepository attendanceRepository) {
        super(attendanceRepository);
        this.attendanceRepository = attendanceRepository;
    }

    /** Lấy điểm danh của một lớp trong một ngày */
    public List<Attendance> findByClassAndDate(Long classId, LocalDate date) {
        try {
            return txManager.runInTransaction(em ->
                    attendanceRepository.findByClassAndDate(em, classId, date));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải điểm danh: " + e.getMessage(), e);
        }
    }

    /** Lấy toàn bộ điểm danh của một lớp */
    public List<Attendance> findByClassId(Long classId) {
        try {
            return txManager.runInTransaction(em ->
                    attendanceRepository.findByClassId(em, classId));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải điểm danh lớp: " + e.getMessage(), e);
        }
    }

    /** Lấy toàn bộ điểm danh của một học viên */
    public List<Attendance> findByStudentId(Long studentId) {
        try {
            return txManager.runInTransaction(em ->
                    attendanceRepository.findByStudentId(em, studentId));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải điểm danh học viên: " + e.getMessage(), e);
        }
    }

    /** Lấy điểm danh của học viên trong một lớp cụ thể */
    public List<Attendance> findByStudentAndClass(Long studentId, Long classId) {
        try {
            return txManager.runInTransaction(em ->
                    attendanceRepository.findByStudentAndClass(em, studentId, classId));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải điểm danh: " + e.getMessage(), e);
        }
    }

    /**
     * Lưu hàng loạt danh sách điểm danh trong một transaction.
     * Dùng merge để upsert: nếu đã có thì update, chưa có thì insert.
     */
    public void saveAll(List<Attendance> list) {
        try {
            txManager.runInTransaction(em -> {
                for (Attendance a : list) {
                    em.merge(a);
                }
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lưu điểm danh: " + e.getMessage(), e);
        }
    }

    /** Các ngày đã có điểm danh của một lớp */
    public List<LocalDate> findAttendanceDatesByClass(Long classId) {
        try {
            return txManager.runInTransaction(em ->
                    attendanceRepository.findAttendanceDatesByClass(em, classId));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải danh sách ngày: " + e.getMessage(), e);
        }
    }

    /** Thống kê tổng hợp vắng mặt theo lớp — dùng cho Admin dashboard */
    public List<Object[]> getAbsenceSummaryByClass() {
        try {
            return txManager.runInTransaction(em ->
                    attendanceRepository.getAbsenceSummaryByClass(em));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải thống kê: " + e.getMessage(), e);
        }
    }

    /** Tỉ lệ điểm danh theo lớp trong khoảng thời gian — dùng cho Admin dashboard */
    public List<Object[]> getAttendanceRateByClass(LocalDate from, LocalDate to) {
        try {
            return txManager.runInTransaction(em ->
                    attendanceRepository.getAttendanceRateByClass(em, from, to));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải tỉ lệ điểm danh: " + e.getMessage(), e);
        }
    }

    /**
     * Tính % chuyên cần của một học viên trong một lớp.
     * = số buổi Present / tổng số buổi đã điểm danh
     */
    public double getAttendanceRate(Long studentId, Long classId) {
        try {
            return txManager.runInTransaction(em -> {
                long total   = attendanceRepository.countByStudentClassStatus(em, studentId, classId, "Present")
                             + attendanceRepository.countByStudentClassStatus(em, studentId, classId, "Absent")
                             + attendanceRepository.countByStudentClassStatus(em, studentId, classId, "Late");
                if (total == 0) return 0.0;
                long present = attendanceRepository.countByStudentClassStatus(em, studentId, classId, "Present");
                return (present * 100.0) / total;
            });
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tính chuyên cần: " + e.getMessage(), e);
        }
    }

    /**
     * Tạo danh sách Attendance trắng cho một lớp + ngày,
     * dựa trên danh sách học viên enrolled được truyền vào.
     * Chỉ tạo cho những học viên chưa có record trong ngày đó.
     */
    public List<Attendance> prepareAttendanceSheet(
            com.company.ems.model.Class clazz,
            LocalDate date,
            List<Student> enrolledStudents) {

        try {
            List<Attendance> sheet = txManager.runInTransaction(em -> {
                List<Attendance> existing =
                        attendanceRepository.findByClassAndDate(em, clazz.getClassId(), date);

                // Map studentId → existing record
                java.util.Map<Long, Attendance> existingMap = new java.util.HashMap<>();
                for (Attendance a : existing) {
                    existingMap.put(a.getStudent().getStudentId(), a);
                }

                // Với mỗi học viên: nếu đã có record thì dùng lại, chưa có thì tạo mới Present
                List<Attendance> result = new java.util.ArrayList<>();
                for (Student s : enrolledStudents) {
                    Attendance a = existingMap.get(s.getStudentId());
                    if (a == null) {
                        a = new Attendance();
                        a.setStudent(s);
                        a.setClazz(clazz);
                        a.setAttendDate(date);
                        a.setStatus("Present");
                    }
                    result.add(a);
                }
                return result;
            });
            return sheet;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi chuẩn bị phiếu điểm danh: " + e.getMessage(), e);
        }
    }
}

