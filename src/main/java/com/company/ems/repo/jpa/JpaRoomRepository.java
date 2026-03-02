package com.company.ems.repo.jpa;

import com.company.ems.model.Room;
import com.company.ems.repo.RoomRepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class JpaRoomRepository extends JpaBaseRepository<Room, Long> implements RoomRepository {

    public JpaRoomRepository() {
        super(Room.class);
    }

    @Override
    public List<Room> findByStatus(EntityManager em, String status) {
        return em.createQuery(
                "SELECT r FROM Room r WHERE r.status = :status ORDER BY r.roomName", Room.class)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public List<Room> findByNameContaining(EntityManager em, String keyword) {
        return em.createQuery(
                "SELECT r FROM Room r WHERE LOWER(r.roomName) LIKE LOWER(:kw) ORDER BY r.roomName", Room.class)
                .setParameter("kw", "%" + keyword + "%")
                .getResultList();
    }
}

