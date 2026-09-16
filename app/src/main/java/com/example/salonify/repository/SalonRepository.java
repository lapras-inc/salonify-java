package com.example.salonify.repository;

import com.example.salonify.entity.Salon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalonRepository extends JpaRepository<Salon, String> {
    List<Salon> findByVisibilityOrderByCreatedAtDesc(String visibility);
    List<Salon> findByVisibilityAndCategoryOrderByCreatedAtDesc(String visibility, String category);
    List<Salon> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
    Page<Salon> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
