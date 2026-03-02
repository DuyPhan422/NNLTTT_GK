package com.company.ems.service;

import java.util.List;

public interface BaseService<T, ID> {
    void save(T entity);
    void update(T entity);
    void delete(ID id);
    T findById(ID id);
    List<T> findAll();
}

