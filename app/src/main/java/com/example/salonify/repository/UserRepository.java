package com.example.salonify.repository;

import com.example.salonify.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<User> findByEmailContainingIgnoreCaseOrderByCreatedAtDesc(String q, Pageable pageable);
    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
