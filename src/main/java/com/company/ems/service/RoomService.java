package com.company.ems.service;

import com.company.ems.model.Room;
import com.company.ems.repo.RoomRepository;

import java.util.List;

public class RoomService extends AbstractBaseService<Room, Long> {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        super(roomRepository);
        this.roomRepository = roomRepository;
    }

    public List<Room> findByStatus(String status) {
        try {
            return txManager.runInTransaction(em -> roomRepository.findByStatus(em, status));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm theo trạng thái: " + e.getMessage(), e);
        }
    }

    public List<Room> findByNameContaining(String keyword) {
        try {
            return txManager.runInTransaction(em -> roomRepository.findByNameContaining(em, keyword));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm kiếm phòng học: " + e.getMessage(), e);
        }
    }
}

