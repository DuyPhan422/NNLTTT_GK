package com.company.ems.service;

import com.company.ems.db.TransactionManager;
import com.company.ems.repo.BaseRepository;

import java.util.List;

public abstract class AbstractBaseService<T, ID> implements BaseService<T, ID> {

    protected final BaseRepository<T, ID> repository;
    protected final TransactionManager txManager;

    protected AbstractBaseService(BaseRepository<T, ID> repository) {
        this.repository = repository;
        this.txManager = new TransactionManager();
    }

    @Override
    public void save(T entity) {
        try {
            txManager.runInTransaction(em -> {
                repository.insert(em, entity);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lưu: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(T entity) {
        try {
            txManager.runInTransaction(em -> {
                repository.update(em, entity);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi cập nhật: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(ID id) {
        try {
            txManager.runInTransaction(em -> {
                repository.delete(em, id);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xóa: " + e.getMessage(), e);
        }
    }

    @Override
    public T findById(ID id) {
        try {
            return txManager.runInTransaction(em -> repository.findById(em, id));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm theo ID: " + e.getMessage(), e);
        }
    }

    @Override
    public List<T> findAll() {
        try {
            return txManager.runInTransaction(em -> repository.findAll(em));
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách: " + e.getMessage(), e);
        }
    }
}

