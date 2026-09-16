package com.example.salonify.repository;

import com.example.salonify.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, String> {
    Optional<EmailVerification> findFirstByEmailAndCodeOrderByCreatedAtDesc(String email, String code);
    Optional<EmailVerification> findFirstByEmailOrderByCreatedAtDesc(String email);
    void deleteByEmail(String email);
}
