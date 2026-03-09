package com.company.ems.repo.jpa;

import com.company.ems.model.Attendance;
import com.company.ems.repo.AttendanceRepository;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

public class JpaAttendanceRepository extends JpaBaseRepository<Attendance, Long>
        implements AttendanceRepository {

    public JpaAttendanceRepository() {
        super(Attendance.class);
    }

    @Override
    public List<Attendance> findByClassAndDate(EntityManager em, Long classId, LocalDate date) {
        return em.createQuery(
                "SELECT a FROM Attendance a " +
                "JOIN FETCH a.student " +
                "WHERE a.clazz.classId = :classId AND a.attendDate = :date " +
                "ORDER BY a.student.fullName", Attendance.class)
                .setParameter("classId", classId)
                .setParameter("date", date)
                .getResultList();
    }

    @Override
    public List<Attendance> findByClassId(EntityManager em, Long classId) {
        return em.createQuery(
                "SELECT a FROM Attendance a " +
                "JOIN FETCH a.student " +
                "WHERE a.clazz.classId = :classId " +
                "ORDER BY a.attendDate DESC, a.student.fullName", Attendance.class)
                .setParameter("classId", classId)
                .getResultList();
    }

    @Override
    public List<Attendance> findByStudentId(EntityManager em, Long studentId) {
        return em.createQuery(
                "SELECT a FROM Attendance a " +
                "JOIN FETCH a.clazz c " +
                "LEFT JOIN FETCH c.course " +
                "WHERE a.student.studentId = :studentId " +
                "ORDER BY a.attendDate DESC", Attendance.class)
                .setParameter("studentId", studentId)
                .getResultList();
    }

    @Override
    public List<Attendance> findByStudentAndClass(EntityManager em, Long studentId, Long classId) {
        return em.createQuery(
                "SELECT a FROM Attendance a " +
                "WHERE a.student.studentId = :studentId AND a.clazz.classId = :classId " +
                "ORDER BY a.attendDate", Attendance.class)
                .setParameter("studentId", studentId)
                .setParameter("classId", classId)
                .getResultList();
    }

    @Override
    public boolean existsByStudentClassDate(EntityManager em, Long studentId, Long classId, LocalDate date) {
        Long count = em.createQuery(
                "SELECT COUNT(a) FROM Attendance a " +
                "WHERE a.student.studentId = :sid AND a.clazz.classId = :cid AND a.attendDate = :date",
                Long.class)
                .setParameter("sid", studentId)
                .setParameter("cid", classId)
                .setParameter("date", date)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public long countByStudentClassStatus(EntityManager em, Long studentId, Long classId, String status) {
        return em.createQuery(
                "SELECT COUNT(a) FROM Attendance a " +
                "WHERE a.student.studentId = :sid AND a.clazz.classId = :cid AND a.status = :status",
                Long.class)
                .setParameter("sid", studentId)
                .setParameter("cid", classId)
                .setParameter("status", status)
                .getSingleResult();
    }

    @Override
    public List<Object[]> getAbsenceSummaryByClass(EntityManager em) {
        // Trả về: [className, courseName, totalSessions, absentCount, presentCount, lateCount]
        return em.createQuery(
                "SELECT c.className, co.courseName, " +
                "COUNT(a), " +
                "SUM(CASE WHEN a.status = 'Absent'  THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN a.status = 'Present' THEN 1 ELSE 0 END), " +
                "SUM(CASE WHEN a.status = 'Late'    THEN 1 ELSE 0 END) " +
                "FROM Attendance a " +
                "JOIN a.clazz c " +
                "JOIN c.course co " +
                "GROUP BY c.classId, c.className, co.courseName " +
                "ORDER BY c.className", Object[].class)
                .getResultList();
    }

    @Override
    public List<Object[]> getAttendanceRateByClass(EntityManager em, LocalDate from, LocalDate to) {
        // Trả về: [classId, className, totalRecords, presentCount]
        return em.createQuery(
                "SELECT c.classId, c.className, COUNT(a), " +
                "SUM(CASE WHEN a.status = 'Present' THEN 1 ELSE 0 END) " +
                "FROM Attendance a " +
                "JOIN a.clazz c " +
                "WHERE a.attendDate BETWEEN :from AND :to " +
                "GROUP BY c.classId, c.className " +
                "ORDER BY c.className", Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    @Override
    public List<LocalDate> findAttendanceDatesByClass(EntityManager em, Long classId) {
        return em.createQuery(
                "SELECT DISTINCT a.attendDate FROM Attendance a " +
                "WHERE a.clazz.classId = :classId " +
                "ORDER BY a.attendDate DESC", LocalDate.class)
                .setParameter("classId", classId)
                .getResultList();
    }
}

