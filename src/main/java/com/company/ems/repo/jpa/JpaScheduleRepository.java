package com.company.ems.repo.jpa;

import com.company.ems.model.Schedule;
import com.company.ems.repo.ScheduleRepository;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class JpaScheduleRepository extends JpaBaseRepository<Schedule, Long> implements ScheduleRepository {

    public JpaScheduleRepository() {
        super(Schedule.class);
    }

    /**
     * Override findById để JOIN FETCH room và clazz,
     * tránh LazyInitializationException khi session đã đóng.
     */
    @Override
    public Schedule findById(EntityManager em, Long id) {
        var list = em.createQuery(
                "SELECT s FROM Schedule s " +
                "LEFT JOIN FETCH s.room " +
                "LEFT JOIN FETCH s.clazz c " +
                "LEFT JOIN FETCH c.course " +
                "LEFT JOIN FETCH c.teacher " +
                "LEFT JOIN FETCH c.room " +
                "WHERE s.scheduleId = :id", Schedule.class)
                .setParameter("id", id)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<Schedule> findByClassId(EntityManager em, Long classId) {
        return em.createQuery(
                "SELECT s FROM Schedule s " +
                "LEFT JOIN FETCH s.room " +
                "LEFT JOIN FETCH s.clazz c " +
                "LEFT JOIN FETCH c.teacher " +
                "WHERE c.classId = :classId " +
                "ORDER BY s.studyDate, s.startTime", Schedule.class)
                .setParameter("classId", classId)
                .getResultList();
    }

    @Override
    public List<Schedule> findByDateRange(EntityManager em, LocalDate from, LocalDate to) {
        return em.createQuery(
                "SELECT s FROM Schedule s " +
                "LEFT JOIN FETCH s.room " +
                "LEFT JOIN FETCH s.clazz " +
                "WHERE s.studyDate >= :from AND s.studyDate <= :to " +
                "ORDER BY s.studyDate, s.startTime", Schedule.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    @Override
    public List<Schedule> findRoomConflicts(EntityManager em,
                                             Long roomId,
                                             LocalDate date,
                                             LocalTime startTime,
                                             LocalTime endTime,
                                             Long excludeScheduleId) {
        String jpql = "SELECT s FROM Schedule s " +
                      "LEFT JOIN FETCH s.room " +
                      "LEFT JOIN FETCH s.clazz c " +
                      "LEFT JOIN FETCH c.teacher " +
                      "WHERE s.room.roomId = :roomId " +
                      "AND s.studyDate = :date " +
                      "AND s.startTime < :endTime " +
                      "AND s.endTime > :startTime " +
                      (excludeScheduleId != null ? "AND s.scheduleId <> :excludeId" : "");

        var query = em.createQuery(jpql, Schedule.class)
                .setParameter("roomId", roomId)
                .setParameter("date", date)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime);

        if (excludeScheduleId != null) {
            query.setParameter("excludeId", excludeScheduleId);
        }
        return query.getResultList();
    }

    @Override
    public List<Schedule> findTeacherConflicts(EntityManager em,
                                                Long teacherId,
                                                LocalDate date,
                                                LocalTime startTime,
                                                LocalTime endTime,
                                                Long excludeScheduleId) {
        String jpql = "SELECT s FROM Schedule s " +
                      "LEFT JOIN FETCH s.room " +
                      "LEFT JOIN FETCH s.clazz c " +
                      "LEFT JOIN FETCH c.teacher " +
                      "WHERE c.teacher.teacherId = :teacherId " +
                      "AND s.studyDate = :date " +
                      "AND s.startTime < :endTime " +
                      "AND s.endTime > :startTime " +
                      (excludeScheduleId != null ? "AND s.scheduleId <> :excludeId" : "");

        var query = em.createQuery(jpql, Schedule.class)
                .setParameter("teacherId", teacherId)
                .setParameter("date", date)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime);

        if (excludeScheduleId != null) {
            query.setParameter("excludeId", excludeScheduleId);
        }
        return query.getResultList();
    }
}

