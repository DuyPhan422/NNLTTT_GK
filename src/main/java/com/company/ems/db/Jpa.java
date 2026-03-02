package com.company.ems.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public final class Jpa {
    private static final EntityManagerFactory EMF =
            Persistence.createEntityManagerFactory("productPU");

    private Jpa() {}

    public static EntityManager em() {
        return EMF.createEntityManager();
    }

    public static void shutdown() {
        EMF.close();
    }
}
