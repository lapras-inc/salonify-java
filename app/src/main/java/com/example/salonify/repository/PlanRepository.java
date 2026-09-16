package com.example.salonify.repository;

import com.example.salonify.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlanRepository extends JpaRepository<Plan, String> {
    List<Plan> findBySalonIdOrderByPriceJpyAsc(String salonId);
    void deleteBySalonId(String salonId);
}
