package com.example.salonify.repository;

import com.example.salonify.entity.Thread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ThreadRepository extends JpaRepository<Thread, String> {
    List<Thread> findBySalonIdOrderByCreatedAtDesc(String salonId);
    Page<Thread> findBySalonIdOrderByCreatedAtDesc(String salonId, Pageable pageable);
    long countBySalonId(String salonId);
    void deleteBySalonId(String salonId);
}
