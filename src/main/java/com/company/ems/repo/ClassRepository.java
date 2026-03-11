package com.company.ems.repo;

import com.company.ems.model.Class;
import com.company.ems.model.Student;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

public interface ClassRepository extends BaseRepository<Class, Long> {
    List<Class> findByStatus(EntityManager em, String status);
    List<Class> findByCourseId(EntityManager em, Long courseId);
    List<Class> findByTeacherId(EntityManager em, Long teacherId);
    List<Class> findStartingFrom(EntityManager em, LocalDate fromDate);

    /** Lấy danh sách Student đang Enrolled của một lớp — trong 1 query, không lazy */
    List<Student> findEnrolledStudents(EntityManager em, Long classId);

    /** Lấy danh sách lớp mà học viên đã đăng ký */
    List<Class> findByStudentId(EntityManager em, Long studentId);
}

