package com.example.salonify.repository;

import com.example.salonify.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, String> {
    List<Post> findBySalonIdOrderByPinnedDescCreatedAtDesc(String salonId);
    List<Post> findBySalonIdOrderByCreatedAtDesc(String salonId);
    List<Post> findBySalonIdAndDraftFalseOrderByPinnedDescCreatedAtDesc(String salonId);
    long countBySalonId(String salonId);
    void deleteBySalonId(String salonId);
}
