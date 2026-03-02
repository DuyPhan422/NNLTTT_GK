package com.company.ems.repo;

import jakarta.persistence.EntityManager;
import java.util.List;

public interface BaseRepository<T, ID> {
    void insert(EntityManager em, T entity);
    void update(EntityManager em, T entity);
    void delete(EntityManager em, ID id);
    T findById(EntityManager em, ID id);
    List<T> findAll(EntityManager em);
}
