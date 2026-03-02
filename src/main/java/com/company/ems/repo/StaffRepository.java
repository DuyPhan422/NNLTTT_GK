package com.company.ems.repo;

import com.company.ems.model.Staff;
import jakarta.persistence.EntityManager;

import java.util.List;

public interface StaffRepository extends BaseRepository<Staff, Long> {
    List<Staff> findByStatus(EntityManager em, String status);
    List<Staff> findByRole(EntityManager em, String role);
    List<Staff> findByFullNameContaining(EntityManager em, String keyword);
}

