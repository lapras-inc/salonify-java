package com.example.salonify.repository;

import com.example.salonify.entity.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, String> {
    Optional<PasswordReset> findFirstByEmailAndCodeOrderByCreatedAtDesc(String email, String code);
    Optional<PasswordReset> findFirstByEmailOrderByCreatedAtDesc(String email);
    void deleteByEmail(String email);
}
