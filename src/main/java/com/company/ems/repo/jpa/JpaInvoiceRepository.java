package com.company.ems.repo.jpa;

import com.company.ems.model.Invoice;
import jakarta.persistence.EntityManager;
import java.util.List;

public class JpaInvoiceRepository extends JpaBaseRepository<Invoice, Long> {
    
    public JpaInvoiceRepository() { 
        super(Invoice.class); 
    }

    // --- GIỮ NGUYÊN HÀM FINDALL ĐÃ SỬA ---
    @Override
    public List<Invoice> findAll(EntityManager em) {
        return em.createQuery(
            "SELECT i FROM Invoice i JOIN FETCH i.student", Invoice.class)
            .getResultList();
    }

    // --- THÊM MỚI HÀM FINDBYID NÀY ---
    @Override
    public Invoice findById(EntityManager em, Long id) {
        try {
            return em.createQuery(
                "SELECT i FROM Invoice i JOIN FETCH i.student WHERE i.invoiceId = :id", Invoice.class)
                .setParameter("id", id)
                .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }
}