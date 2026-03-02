package com.company.ems.repo;

import com.company.ems.model.Room;
import jakarta.persistence.EntityManager;

import java.util.List;

public interface RoomRepository extends BaseRepository<Room, Long> {
    List<Room> findByStatus(EntityManager em, String status);
    List<Room> findByNameContaining(EntityManager em, String keyword);
}

