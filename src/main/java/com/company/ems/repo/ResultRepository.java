package com.company.ems.repo;

import com.company.ems.model.Result;
import jakarta.persistence.EntityManager;

import java.util.List;

public interface ResultRepository extends BaseRepository<Result, Long> {

    /** Toàn bộ kết quả của một lớp, sắp theo tên học viên */
    List<Result> findByClassId(EntityManager em, Long classId);

    /** Kết quả của một học viên trong một lớp cụ thể */
    List<Result> findByStudentAndClass(EntityManager em, Long studentId, Long classId);

    /** Toàn bộ kết quả của một học viên */
    List<Result> findByStudentId(EntityManager em, Long studentId);
}

