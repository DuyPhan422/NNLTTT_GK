package com.company.ems.service;

import com.company.ems.model.Staff;
import com.company.ems.repo.StaffRepository;

import java.util.List;

public class StaffService extends AbstractBaseService<Staff, Long> {

    private final StaffRepository staffRepository;

    public StaffService(StaffRepository staffRepository) {
        super(staffRepository);
        this.staffRepository = staffRepository;
    }

    public List<Staff> findByStatus(String status) {
        try {
            return txManager.runInTransaction(em -> staffRepository.findByStatus(em, status));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm theo trạng thái: " + e.getMessage(), e);
        }
    }

    public List<Staff> findByRole(String role) {
        try {
            return txManager.runInTransaction(em -> staffRepository.findByRole(em, role));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm theo vai trò: " + e.getMessage(), e);
        }
    }

    public List<Staff> findByFullNameContaining(String keyword) {
        try {
            return txManager.runInTransaction(em -> staffRepository.findByFullNameContaining(em, keyword));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm kiếm nhân viên: " + e.getMessage(), e);
        }
    }
}

