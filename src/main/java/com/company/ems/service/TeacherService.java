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

    @Override
    public void update(Teacher entity) {
        try {
            txManager.runInTransaction(em -> {
                if ("Không hoạt động".equals(entity.getStatus()) || "Nghỉ việc".equals(entity.getStatus())) {
                    Long count = em.createQuery(
                        "SELECT COUNT(c) FROM Class c " +
                        "WHERE c.teacher.teacherId = :teacherId " +
                        "AND c.status IN ('Lên kế hoạch', 'Mở lớp', 'Đang diễn ra')", Long.class)
                        .setParameter("teacherId", entity.getTeacherId())
                        .getSingleResult();
                    if (count > 0) {
                        throw new RuntimeException("Không thể vô hiệu hóa giáo viên vì đang có " + count + " lớp học được phân công.");
                    }
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
                Long count = em.createQuery(
                    "SELECT COUNT(c) FROM Class c " +
                    "WHERE c.teacher.teacherId = :teacherId " +
                    "AND c.status IN ('Lên kế hoạch', 'Mở lớp', 'Đang diễn ra')", Long.class)
                    .setParameter("teacherId", id)
                    .getSingleResult();
                if (count > 0) {
                    throw new RuntimeException("Không thể xóa giáo viên vì đang có " + count + " lớp học được phân công.");
                }
                repository.delete(em, id);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}

