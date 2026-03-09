package com.company.ems.repo.jpa;

import com.company.ems.model.UserAccount;
import com.company.ems.repo.UserAccountRepository;
import jakarta.persistence.EntityManager;

import java.util.Optional;

public class JpaUserAccountRepository extends JpaBaseRepository<UserAccount, Long>
        implements UserAccountRepository {

    public JpaUserAccountRepository() {
        super(UserAccount.class);
    }

    @Override
    public Optional<UserAccount> findByUsername(EntityManager em, String username) {
        return em.createQuery(
                        "SELECT u FROM UserAccount u " +
                        "LEFT JOIN FETCH u.student " +
                        "LEFT JOIN FETCH u.teacher " +
                        "LEFT JOIN FETCH u.staff " +
                        "WHERE u.username = :username AND u.isActive = true",
                        UserAccount.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existsByUsername(EntityManager em, String username) {
        Long count = em.createQuery(
                        "SELECT COUNT(u) FROM UserAccount u WHERE u.username = :username",
                        Long.class)
                .setParameter("username", username)
                .getSingleResult();
        return count > 0;
    }
}

