package com.company.ems.service;
import com.company.ems.model.Invoice;
import com.company.ems.repo.jpa.JpaInvoiceRepository;
public class InvoiceService extends AbstractBaseService<Invoice, Long> {
    public InvoiceService(JpaInvoiceRepository repository) 
    	{ super(repository); }
}