package com.company.ems.repo.jpa;

import com.company.ems.model.Student;
import com.company.ems.repo.StudentRepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class JpaStudentRepository extends JpaBaseRepository<Student, Long> implements StudentRepository {

    public JpaStudentRepository() {
        super(Student.class); // Báo cho JPA biết đây là Repository của bảng Student
    }

    @Override
    public List<Student> findByStatus(EntityManager em, String status) {
        return em.createQuery(
                "SELECT s FROM Student s WHERE s.status = :status ORDER BY s.fullName", Student.class)
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public List<Student> findByFullNameContaining(EntityManager em, String keyword) {
        return em.createQuery(
                "SELECT s FROM Student s WHERE LOWER(s.fullName) LIKE LOWER(:kw) ORDER BY s.fullName", Student.class)
                .setParameter("kw", "%" + keyword + "%")
                .getResultList();
    }

    @Override
    public List<Student> findByPhone(EntityManager em, String phone) {
        return em.createQuery(
                "SELECT s FROM Student s WHERE s.phone = :phone", Student.class)
                .setParameter("phone", phone)
                .getResultList();
    }
}
