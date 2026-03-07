package com.company.ems.service;

import com.company.ems.model.Payment;
import com.company.ems.repo.jpa.JpaPaymentRepository;

public class PaymentService extends AbstractBaseService<Payment, Long> {
    public PaymentService(JpaPaymentRepository repository) 
    	{ super(repository); }
}