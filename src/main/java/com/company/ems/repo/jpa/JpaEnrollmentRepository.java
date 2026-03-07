package com.company.ems.repo.jpa;

import com.company.ems.model.Enrollment;
import com.company.ems.repo.EnrollmentRepository;

public class JpaEnrollmentRepository extends JpaBaseRepository<Enrollment, Long> implements EnrollmentRepository {
    
    public JpaEnrollmentRepository() {
        super(Enrollment.class);
    }
}