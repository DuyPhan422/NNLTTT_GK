package com.company.ems.service;

import com.company.ems.model.Enrollment;
import com.company.ems.repo.EnrollmentRepository;

public class EnrollmentService extends AbstractBaseService<Enrollment, Long> {
    
    // Chỉ truyền đúng Repository vào, đúng với kiến trúc hiện tại của dự án
    public EnrollmentService(EnrollmentRepository repository) {
        super(repository);
    }
}