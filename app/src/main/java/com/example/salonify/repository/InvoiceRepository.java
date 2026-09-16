package com.example.salonify.repository;

import com.example.salonify.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {
    List<Invoice> findByMembershipIdOrderByCreatedAtDesc(String membershipId);
    List<Invoice> findByMembershipIdInAndStatusOrderByCreatedAtDesc(List<String> membershipIds, String status);
    List<Invoice> findByMembershipIdInOrderByCreatedAtDesc(List<String> membershipIds);
    List<Invoice> findByStatus(String status);
    Optional<Invoice> findFirstByMembershipIdOrderByCreatedAtDesc(String membershipId);
    void deleteByMembershipIdIn(List<String> membershipIds);
}
