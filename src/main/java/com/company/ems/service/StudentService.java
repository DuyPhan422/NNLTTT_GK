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

    @Override
    public void update(Student entity) {
        try {
            txManager.runInTransaction(em -> {
                if ("Không hoạt động".equals(entity.getStatus()) || "Nghỉ học".equals(entity.getStatus())) {
                    Long count = em.createQuery(
                        "SELECT COUNT(e) FROM Enrollment e " +
                        "WHERE e.student.studentId = :studentId " +
                        "AND e.status IN ('Đã đăng ký', 'Đã thanh toán') " +
                        "AND e.clazz.status NOT IN ('Hoàn thành', 'Huỷ lớp')", Long.class)
                        .setParameter("studentId", entity.getStudentId())
                        .getSingleResult();
                    if (count > 0) {
                        throw new RuntimeException("Không thể vô hiệu hóa học viên vì đang có " + count + " lớp học đang theo học hoặc nợ nần.");
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
                    "SELECT COUNT(e) FROM Enrollment e " +
                    "WHERE e.student.studentId = :studentId " +
                    "AND e.status IN ('Đã đăng ký', 'Đã thanh toán') " +
                    "AND e.clazz.status NOT IN ('Hoàn thành', 'Huỷ lớp')", Long.class)
                    .setParameter("studentId", id)
                    .getSingleResult();
                if (count > 0) {
                    throw new RuntimeException("Không thể xóa học viên vì đang có " + count + " lớp học đang theo học hoặc nợ nần.");
                }
                repository.delete(em, id);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}

