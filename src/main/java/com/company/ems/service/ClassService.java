package com.company.ems.service;

import com.company.ems.model.Class;
import com.company.ems.model.Student;
import com.company.ems.repo.ClassRepository;

import java.time.LocalDate;
import java.util.List;

public class ClassService extends AbstractBaseService<Class, Long> {

    private final ClassRepository classRepository;

    public ClassService(ClassRepository classRepository) {
        super(classRepository);
        this.classRepository = classRepository;
    }

    public List<Class> findByStatus(String status) {
        try {
            return txManager.runInTransaction(em -> classRepository.findByStatus(em, status));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm theo trạng thái lớp: " + e.getMessage(), e);
        }
    }

    public List<Class> findByCourseId(Long courseId) {
        try {
            return txManager.runInTransaction(em -> classRepository.findByCourseId(em, courseId));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm lớp theo khóa học: " + e.getMessage(), e);
        }
    }

    public List<Class> findByTeacherId(Long teacherId) {
        try {
            return txManager.runInTransaction(em -> classRepository.findByTeacherId(em, teacherId));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm lớp theo giáo viên: " + e.getMessage(), e);
        }
    }

    public List<Class> findStartingFrom(LocalDate fromDate) {
        try {
            return txManager.runInTransaction(em -> classRepository.findStartingFrom(em, fromDate));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm lớp từ ngày: " + e.getMessage(), e);
        }
    }

    /** Lấy danh sách học viên đang Enrolled của một lớp — trong 1 transaction */
    public List<Student> findEnrolledStudents(Long classId) {
        try {
            return txManager.runInTransaction(em -> classRepository.findEnrolledStudents(em, classId));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách học viên: " + e.getMessage(), e);
        }
    }
}

