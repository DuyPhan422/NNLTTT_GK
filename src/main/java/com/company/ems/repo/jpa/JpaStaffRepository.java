package com.company.ems.repo.jpa;

import com.company.ems.model.Staff;
import com.company.ems.repo.StaffRepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class JpaStaffRepository extends JpaBaseRepository<Staff, Long> implements StaffRepository {

    public JpaStaffRepository() {
        super(Staff.class);
    }

    @Override
    public List<Staff> findByStatus(EntityManager em, String status) {
        return em.createQuery(
                "SELECT s FROM Staff s WHERE s.status = :status ORDER BY s.fullName", Staff.class)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public List<Staff> findByRole(EntityManager em, String role) {
        return em.createQuery(
                "SELECT s FROM Staff s WHERE s.role = :role ORDER BY s.fullName", Staff.class)
                .setParameter("role", role)
                .getResultList();
    }

    @Override
    public List<Staff> findByFullNameContaining(EntityManager em, String keyword) {
        return em.createQuery(
                "SELECT s FROM Staff s WHERE LOWER(s.fullName) LIKE LOWER(:kw) ORDER BY s.fullName", Staff.class)
                .setParameter("kw", "%" + keyword + "%")
                .getResultList();
    }
}

