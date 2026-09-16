package com.example.salonify.repository;

import com.example.salonify.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, String> {
    List<Report> findAllByOrderByCreatedAtDesc();
    void deleteByThreadIdIn(List<String> threadIds);
}
