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

    @Override
    public void update(Course entity) {
        try {
            txManager.runInTransaction(em -> {
                if ("Không hoạt động".equals(entity.getStatus())) {
                    Long activeClassCount = em.createQuery(
                        "SELECT COUNT(c) FROM Class c " +
                        "WHERE c.course.courseId = :courseId " +
                        "AND c.status = 'Đang diễn ra'", Long.class)
                        .setParameter("courseId", entity.getCourseId())
                        .getSingleResult();
                    if (activeClassCount > 0) {
                        throw new RuntimeException("Không thể vô hiệu hóa khóa học vì đang có " + activeClassCount + " lớp học đang diễn ra.");
                    }

                    Long paidEnrollmentCount = em.createQuery(
                        "SELECT COUNT(e) FROM Enrollment e " +
                        "WHERE e.clazz.course.courseId = :courseId " +
                        "AND e.clazz.status = 'Mở lớp' " +
                        "AND e.status = 'Đã thanh toán'", Long.class)
                        .setParameter("courseId", entity.getCourseId())
                        .getSingleResult();
                    if (paidEnrollmentCount > 0) {
                        throw new RuntimeException("Không thể vô hiệu hóa khóa học vì có lớp mở đã bắt đầu thu phí.");
                    }

                    em.createQuery(
                        "UPDATE Class c SET c.status = 'Huỷ lớp' " +
                        "WHERE c.course.courseId = :courseId " +
                        "AND c.status IN ('Lên kế hoạch', 'Mở lớp')")
                        .setParameter("courseId", entity.getCourseId())
                        .executeUpdate();
                }
                repository.update(em, entity);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void delete(Long id) {
        try {
            txManager.runInTransaction(em -> {
                Long activeClassCount = em.createQuery(
                    "SELECT COUNT(c) FROM Class c " +
                    "WHERE c.course.courseId = :courseId " +
                    "AND c.status = 'Đang diễn ra'", Long.class)
                    .setParameter("courseId", id)
                    .getSingleResult();
                if (activeClassCount > 0) {
                    throw new RuntimeException("Không thể xóa khóa học vì đang có " + activeClassCount + " lớp học đang diễn ra.");
                }

                Long paidEnrollmentCount = em.createQuery(
                    "SELECT COUNT(e) FROM Enrollment e " +
                    "WHERE e.clazz.course.courseId = :courseId " +
                    "AND e.clazz.status = 'Mở lớp' " +
                    "AND e.status = 'Đã thanh toán'", Long.class)
                    .setParameter("courseId", id)
                    .getSingleResult();
                if (paidEnrollmentCount > 0) {
                    throw new RuntimeException("Không thể xóa khóa học vì có lớp mở đã bắt đầu thu phí.");
                }

                em.createQuery(
                    "UPDATE Class c SET c.status = 'Huỷ lớp' " +
                    "WHERE c.course.courseId = :courseId " +
                    "AND c.status IN ('Lên kế hoạch', 'Mở lớp')")
                    .setParameter("courseId", id)
                    .executeUpdate();

                repository.delete(em, id);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}

