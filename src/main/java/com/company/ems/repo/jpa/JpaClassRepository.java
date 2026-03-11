package com.company.ems.repo.jpa;

import com.company.ems.model.Class;
import com.company.ems.model.Student;
import com.company.ems.repo.ClassRepository;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

public class JpaClassRepository extends JpaBaseRepository<Class, Long> implements ClassRepository {

    public JpaClassRepository() {
        super(Class.class);
    }

    /**
     * Override findAll để JOIN FETCH course, teacher, room
     * tránh LazyInitializationException khi session đã đóng.
     */
    @Override
    public List<Class> findAll(EntityManager em) {
        return em.createQuery(
                "SELECT c FROM Class c " +
                "LEFT JOIN FETCH c.course " +
                "LEFT JOIN FETCH c.teacher " +
                "LEFT JOIN FETCH c.room " +
                "ORDER BY c.startDate", Class.class)
                .getResultList();
    }

    /**
     * Override findById để JOIN FETCH đầy đủ associations.
     */
    @Override
    public Class findById(EntityManager em, Long id) {
        var list = em.createQuery(
                "SELECT c FROM Class c " +
                "LEFT JOIN FETCH c.course " +
                "LEFT JOIN FETCH c.teacher " +
                "LEFT JOIN FETCH c.room " +
                "WHERE c.classId = :id", Class.class)
                .setParameter("id", id)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<Class> findByStatus(EntityManager em, String status) {
        return em.createQuery(
                        "SELECT c FROM Class c WHERE c.status = :status ORDER BY c.startDate", Class.class)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public List<Class> findByCourseId(EntityManager em, Long courseId) {
        return em.createQuery(
                        "SELECT c FROM Class c WHERE c.course.courseId = :courseId ORDER BY c.startDate", Class.class)
                .setParameter("courseId", courseId)
                .getResultList();
    }

    @Override
    public List<Class> findByTeacherId(EntityManager em, Long teacherId) {
        return em.createQuery(
                        "SELECT c FROM Class c " +
                        "LEFT JOIN FETCH c.course " +
                        "LEFT JOIN FETCH c.teacher " +
                        "LEFT JOIN FETCH c.room " +
                        "WHERE c.teacher.teacherId = :teacherId ORDER BY c.startDate", Class.class)
                .setParameter("teacherId", teacherId)
                .getResultList();
    }

    @Override
    public List<Student> findEnrolledStudents(EntityManager em, Long classId) {
        // SELECT trực tiếp e.student — Hibernate load đầy đủ Student entity trong 1 query
        return em.createQuery(
                "SELECT s FROM Enrollment e " +
                "JOIN e.student s " +
                "WHERE e.clazz.classId = :classId " +
                "AND e.status = 'Enrolled' " +
                "ORDER BY s.fullName", Student.class)
                .setParameter("classId", classId)
                .getResultList();
    }

    @Override
    public List<Class> findStartingFrom(EntityManager em, LocalDate fromDate) {
        return em.createQuery(
                        "SELECT c FROM Class c WHERE c.startDate >= :fromDate ORDER BY c.startDate", Class.class)
                .setParameter("fromDate", fromDate)
                .getResultList();
    }

    @Override
    public List<Class> findByStudentId(EntityManager em, Long studentId) {
        return em.createQuery(
                "SELECT c FROM Class c " +
                "LEFT JOIN FETCH c.course " +
                "LEFT JOIN FETCH c.teacher " +
                "LEFT JOIN FETCH c.room " +
                "WHERE EXISTS (SELECT e FROM Enrollment e WHERE e.clazz = c AND e.student.studentId = :studentId) " +
                "ORDER BY c.startDate", Class.class)
                .setParameter("studentId", studentId)
                .getResultList();
    }
}

