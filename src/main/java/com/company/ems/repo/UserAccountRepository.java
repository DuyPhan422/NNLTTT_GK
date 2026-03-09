package com.company.ems.repo;

import com.company.ems.model.UserAccount;
import jakarta.persistence.EntityManager;

import java.util.Optional;

public interface UserAccountRepository extends BaseRepository<UserAccount, Long> {

    /** Tìm theo username — dùng cho đăng nhập */
    Optional<UserAccount> findByUsername(EntityManager em, String username);

    /** Kiểm tra username đã tồn tại chưa */
    boolean existsByUsername(EntityManager em, String username);
}

