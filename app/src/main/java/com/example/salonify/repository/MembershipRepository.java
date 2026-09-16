package com.example.salonify.repository;

import com.example.salonify.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, String> {
    Optional<Membership> findByUserIdAndSalonId(String userId, String salonId);
    List<Membership> findByUserIdAndStatusOrderByJoinedAtDesc(String userId, String status);
    List<Membership> findByUserId(String userId);
    List<Membership> findBySalonIdOrderByJoinedAtDesc(String salonId);
    List<Membership> findBySalonIdAndStatus(String salonId, String status);
    long countBySalonIdAndStatus(String salonId, String status);
    long countByStatus(String status);
    void deleteBySalonId(String salonId);
}
