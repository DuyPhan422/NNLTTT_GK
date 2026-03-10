package com.company.ems.repo.jpa;
import com.company.ems.model.Payment;
import jakarta.persistence.EntityManager;
import java.util.List;

public class JpaPaymentRepository extends JpaBaseRepository<Payment, Long> {
    public JpaPaymentRepository() { super(Payment.class); }

    @Override
    public List<Payment> findAll(EntityManager em) {
        return em.createQuery(
            "SELECT p FROM Payment p LEFT JOIN FETCH p.student", Payment.class)
            .getResultList();
    }
}