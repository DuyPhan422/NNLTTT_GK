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

    @Override
    public void update(Room entity) {
        try {
            txManager.runInTransaction(em -> {
                if ("Không hoạt động".equals(entity.getStatus())) {
                    Long count = em.createQuery(
                        "SELECT COUNT(c) FROM Class c " +
                        "WHERE c.room.roomId = :roomId " +
                        "AND c.status IN ('Lên kế hoạch', 'Mở lớp', 'Đang diễn ra')", Long.class)
                        .setParameter("roomId", entity.getRoomId())
                        .getSingleResult();
                    if (count > 0) {
                        throw new RuntimeException("Không thể vô hiệu hóa phòng vì đang có " + count + " lớp học sử dụng.");
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
                    "WHERE c.room.roomId = :roomId " +
                    "AND c.status IN ('Lên kế hoạch', 'Mở lớp', 'Đang diễn ra')", Long.class)
                    .setParameter("roomId", id)
                    .getSingleResult();
                if (count > 0) {
                    throw new RuntimeException("Không thể xóa phòng vì đang có " + count + " lớp học sử dụng.");
                }
                repository.delete(em, id);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}

