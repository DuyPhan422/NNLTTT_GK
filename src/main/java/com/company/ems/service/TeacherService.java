package com.company.ems.service;

import com.company.ems.model.Teacher;
import com.company.ems.repo.TeacherRepository;

import java.util.List;

public class TeacherService extends AbstractBaseService<Teacher, Long> {

    private final TeacherRepository teacherRepository;

    public TeacherService(TeacherRepository teacherRepository) {
        super(teacherRepository);
        this.teacherRepository = teacherRepository;
    }

    public List<Teacher> findByStatus(String status) {
        try {
            return txManager.runInTransaction(em -> teacherRepository.findByStatus(em, status));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm theo trạng thái: " + e.getMessage(), e);
        }
    }

    public List<Teacher> findBySpecialty(String specialty) {
        try {
            return txManager.runInTransaction(em -> teacherRepository.findBySpecialty(em, specialty));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm theo chuyên môn: " + e.getMessage(), e);
        }
    }
}

