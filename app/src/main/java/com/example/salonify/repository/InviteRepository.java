package com.example.salonify.repository;

import com.example.salonify.entity.Invite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InviteRepository extends JpaRepository<Invite, String> {
    Optional<Invite> findByCode(String code);
    List<Invite> findBySalonIdOrderByCreatedAtDesc(String salonId);
    boolean existsBySalonId(String salonId);
    void deleteBySalonId(String salonId);
}
