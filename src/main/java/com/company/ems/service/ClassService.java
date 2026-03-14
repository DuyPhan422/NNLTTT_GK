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

    /** Lấy danh sách lớp mà một học viên đã đăng ký (Enrolled) */
    public List<Class> findByStudentId(Long studentId) {
        try {
            return txManager.runInTransaction(em -> classRepository.findByStudentId(em, studentId));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách lớp của học viên: " + e.getMessage(), e);
        }
    }

    public List<Class> findPaidClassesByStudentId(Long studentId) {
        try {
            return txManager.runInTransaction(em -> classRepository.findPaidClassesByStudentId(em, studentId));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách lớp đã thanh toán: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Class entity) {
        try {
            txManager.runInTransaction(em -> {
                if ("Hủy lớp".equals(entity.getStatus()) || "Huỷ lớp".equals(entity.getStatus())) {
                    Class existing = em.find(Class.class, entity.getClassId());
                    if (existing != null) {
                        String currentStatus = existing.getStatus();
                        if ("Đang diễn ra".equals(currentStatus) || "Hoàn thành".equals(currentStatus)) {
                            throw new RuntimeException("Không thể hủy lớp học đang diễn ra hoặc đã hoàn thành.");
                        }
                    }

                    Long paidEnrollmentCount = em.createQuery(
                        "SELECT COUNT(e) FROM Enrollment e " +
                        "WHERE e.clazz.classId = :classId " +
                        "AND e.status = 'Đã thanh toán'", Long.class)
                        .setParameter("classId", entity.getClassId())
                        .getSingleResult();
                    if (paidEnrollmentCount > 0) {
                        throw new RuntimeException("Không thể hủy lớp vì học viên đã đóng tiền. Vui lòng hoàn tiền trước.");
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
                Class existing = em.find(Class.class, id);
                if (existing != null) {
                    String currentStatus = existing.getStatus();
                    if ("Đang diễn ra".equals(currentStatus) || "Hoàn thành".equals(currentStatus)) {
                        throw new RuntimeException("Không thể xóa lớp học đang diễn ra hoặc đã hoàn thành.");
                    }
                }

                Long paidEnrollmentCount = em.createQuery(
                    "SELECT COUNT(e) FROM Enrollment e " +
                    "WHERE e.clazz.classId = :classId " +
                    "AND e.status = 'Đã thanh toán'", Long.class)
                    .setParameter("classId", id)
                    .getSingleResult();
                if (paidEnrollmentCount > 0) {
                    throw new RuntimeException("Không thể xóa lớp vì học viên đã đóng tiền. Vui lòng hoàn tiền trước.");
                }

                repository.delete(em, id);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void syncAutoClassStatuses() {
        try {
            txManager.runInTransaction(em -> {
                LocalDate today = LocalDate.now();

                em.createQuery(
                    "UPDATE Class c SET c.status = 'Đang diễn ra' " +
                    "WHERE c.status = 'Mở lớp' AND c.startDate <= :today")
                    .setParameter("today", today)
                    .executeUpdate();

                em.createQuery(
                    "UPDATE Class c SET c.status = 'Hoàn thành' " +
                    "WHERE c.status = 'Đang diễn ra' AND c.endDate IS NOT NULL AND c.endDate < :today")
                    .setParameter("today", today)
                    .executeUpdate();

                return null;
            });
        } catch (Exception e) {
            System.err.println("Lỗi đồng bộ trạng thái lớp: " + e.getMessage());
        }
    }
}

