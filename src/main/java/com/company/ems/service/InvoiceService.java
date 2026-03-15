package com.company.ems.service;
import com.company.ems.model.Invoice;
import com.company.ems.repo.jpa.JpaInvoiceRepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class InvoiceService extends AbstractBaseService<Invoice, Long> {
    private final JpaInvoiceRepository invoiceRepository;
    
    public InvoiceService(JpaInvoiceRepository repository) {
        super(repository); 
        this.invoiceRepository = repository;
    }

    public List<Invoice> findByStudentIdAndStatus(Long studentId, String status) {
        try {
            return txManager.runInTransaction(em -> invoiceRepository.findByStudentIdAndStatus(em, studentId, status));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải kết quả lớp: " + e.getMessage(), e);
        }
    }

    public List<Invoice> findByStudentIdAndStatusOrderByCreatedAtDesc(Long studentId, String status) {
        try {
            return txManager.runInTransaction(em -> invoiceRepository.findByStudentIdAndStatusOrderByCreatedAtDesc(em, studentId, status));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải kết quả lớp: " + e.getMessage(), e);
        }
    }
}