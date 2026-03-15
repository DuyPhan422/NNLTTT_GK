package com.company.ems.service;

import com.company.ems.model.Enrollment;
import com.company.ems.repo.EnrollmentRepository;

import java.util.List;

public class EnrollmentService extends AbstractBaseService<Enrollment, Long> {

    private final EnrollmentRepository enrollmentRepository;

    // Chỉ truyền đúng Repository vào, đúng với kiến trúc hiện tại của dự án
    public EnrollmentService(EnrollmentRepository repository) {
        super(repository);
        this.enrollmentRepository = repository;
    }

    public List<Enrollment> findByStudentIdAndStatus(Long studentId, String status) {
        try {
            return txManager
                    .runInTransaction(em -> enrollmentRepository.findByStudentIdAndStatus(em, studentId, status));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm theo id và trạng thái: " + e.getMessage(), e);
        }
    }

    public List<Enrollment> findByStudentId(Long studentId) {
        try {
            return txManager.runInTransaction(em -> enrollmentRepository.findByStudentId(em, studentId));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm theo id: " + e.getMessage(), e);
        }
    }

    public List<Enrollment> findByClassIdAndStatus(Long classId, String status) {
        try {
            return txManager.runInTransaction(em -> enrollmentRepository.findByClassIdAndStatus(em, classId, status));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tìm theo lớp và trạng thái: " + e.getMessage(), e);
        }
    }
}