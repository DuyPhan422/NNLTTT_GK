package com.company.ems.repo;

import com.company.ems.model.Invoice;
import jakarta.persistence.EntityManager;

import java.util.List;

public interface InvoiceRepository extends BaseRepository<Invoice, Long> {
    public List<Invoice> findAll(EntityManager em);
    public Invoice findById(EntityManager em, Long id);
    public List<Invoice> findByStudentIdAndStatus(EntityManager em, Long studentId, String status);
    public List<Invoice> findByStudentIdAndStatusOrderByCreatedAtDesc(EntityManager em, Long studentId, String status);
}
