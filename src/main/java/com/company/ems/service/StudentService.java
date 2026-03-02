package com.company.ems.service;

import com.company.ems.model.Student;
import com.company.ems.repo.StudentRepository;

import java.util.List;

public class StudentService extends AbstractBaseService<Student, Long> {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        super(studentRepository);
        this.studentRepository = studentRepository;
    }

    public List<Student> findByStatus(String status) {
        try {
            return txManager.runInTransaction(em -> studentRepository.findByStatus(em, status));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm theo trạng thái: " + e.getMessage(), e);
        }
    }

    public List<Student> findByFullNameContaining(String keyword) {
        try {
            return txManager.runInTransaction(em -> studentRepository.findByFullNameContaining(em, keyword));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm kiếm học viên: " + e.getMessage(), e);
        }
    }

    public List<Student> findByPhone(String phone) {
        try {
            return txManager.runInTransaction(em -> studentRepository.findByPhone(em, phone));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm theo số điện thoại: " + e.getMessage(), e);
        }
    }
}

