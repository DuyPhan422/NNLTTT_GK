package com.company.ems.repo.jpa;
import com.company.ems.model.Payment;
public class JpaPaymentRepository extends JpaBaseRepository<Payment, Long> {
    public JpaPaymentRepository() { super(Payment.class); }
}