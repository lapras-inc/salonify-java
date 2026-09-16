package com.example.salonify.repository;

import com.example.salonify.entity.PostReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PostReactionRepository extends JpaRepository<PostReaction, String> {
    Optional<PostReaction> findByPostIdAndUserId(String postId, String userId);
    long countByPostId(String postId);
    void deleteByPostIdIn(List<String> postIds);
}
