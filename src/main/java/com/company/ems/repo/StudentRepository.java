package com.company.ems.repo;

import com.company.ems.model.Student;
import jakarta.persistence.EntityManager;

import java.util.List;

public interface StudentRepository extends BaseRepository<Student, Long> {
    List<Student> findByStatus(EntityManager em, String status);
    List<Student> findByFullNameContaining(EntityManager em, String keyword);
    List<Student> findByPhone(EntityManager em, String phone);
}

