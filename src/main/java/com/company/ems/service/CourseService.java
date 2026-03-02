package com.company.ems.service;

import com.company.ems.model.Course;
import com.company.ems.repo.CourseRepository;

import java.util.List;

public class CourseService extends AbstractBaseService<Course, Long> {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        super(courseRepository);
        this.courseRepository = courseRepository;
    }

    public List<Course> findByStatus(String status) {
        try {
            return txManager.runInTransaction(em -> courseRepository.findByStatus(em, status));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm theo trạng thái: " + e.getMessage(), e);
        }
    }

    public List<Course> findByLevel(String level) {
        try {
            return txManager.runInTransaction(em -> courseRepository.findByLevel(em, level));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm theo cấp độ: " + e.getMessage(), e);
        }
    }

    public List<Course> findByNameContaining(String keyword) {
        try {
            return txManager.runInTransaction(em -> courseRepository.findByNameContaining(em, keyword));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm kiếm khóa học: " + e.getMessage(), e);
        }
    }
}

