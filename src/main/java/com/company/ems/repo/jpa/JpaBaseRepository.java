package com.company.ems.repo.jpa;

import com.company.ems.repo.BaseRepository;
import jakarta.persistence.EntityManager;
import java.util.List;

public abstract class JpaBaseRepository<T, ID> implements BaseRepository<T, ID> {

    private final Class<T> entityClass;

    public JpaBaseRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public void insert(EntityManager em, T entity) {
        em.persist(entity);
    }

    @Override
    public void update(EntityManager em, T entity) {
        em.merge(entity);
    }

    @Override
    public void delete(EntityManager em, ID id) {
        T entity = em.find(entityClass, id);
        if (entity != null) {
            em.remove(entity);
        }
    }

    @Override
    public T findById(EntityManager em, ID id) {
        return em.find(entityClass, id);
    }

    @Override
    public List<T> findAll(EntityManager em) {
        return em.createQuery("select e from " + entityClass.getSimpleName() + " e", entityClass).getResultList();
    }
}
