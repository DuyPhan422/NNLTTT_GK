package com.company.ems.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.company.ems.model.UserAccount;
import com.company.ems.repo.UserAccountRepository;

import java.util.Optional;

/**
 * Service xử lý đăng nhập và xác thực người dùng.
 * Password được hash bằng BCrypt (cost factor 12) — an toàn hơn SHA-256.
 */
public class UserAccountService extends AbstractBaseService<UserAccount, Long> {

    private final UserAccountRepository userAccountRepository;

    public UserAccountService(UserAccountRepository repository) {
        super(repository);
        this.userAccountRepository = repository;
    }

    /**
     * Xác thực đăng nhập.
     * BCrypt.verifyer() tự động so sánh — không cần hash thủ công rồi so sánh chuỗi.
     * @return UserAccount nếu đúng username/password, empty nếu sai.
     */
    public Optional<UserAccount> login(String username, String rawPassword) {
        try {
            return txManager.runInTransaction(em -> {
                Optional<UserAccount> account = userAccountRepository.findByUsername(em, username);
                if (account.isEmpty()) return Optional.empty();

                UserAccount ua = account.get();

                // BCrypt tự xử lý salt — chỉ cần gọi verify()
                BCrypt.Result result = BCrypt.verifyer()
                        .verify(rawPassword.toCharArray(), ua.getPasswordHash());

                return result.verified ? Optional.of(ua) : Optional.empty();
            });
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đăng nhập: " + e.getMessage(), e);
        }
    }

    /**
     * Hash mật khẩu bằng BCrypt (cost=12).
     * Dùng khi tạo tài khoản mới hoặc đổi mật khẩu.
     * Cost 12 = ~250ms/hash — đủ chậm để chống brute-force.
     */
    public static String hashPassword(String rawPassword) {
        return BCrypt.withDefaults().hashToString(12, rawPassword.toCharArray());
    }

    /**
     * Kiểm tra username đã tồn tại chưa.
     */
    public boolean existsByUsername(String username) {
        try {
            return txManager.runInTransaction(em ->
                    userAccountRepository.existsByUsername(em, username));
        } catch (Exception e) {
            return false;
        }
    }
}

