package com.company.ems.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public class TransactionManager {

    public <T> T runInTransaction(JpaWork<T> work) throws Exception {
        EntityManager em = Jpa.em();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();
            T result = work.execute(em);
            tx.commit();
            return result;
        } catch (Exception ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } finally {
            em.close();
        }
    }
}
