package com.example.salonify.repository;

import com.example.salonify.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String> {
    List<Comment> findByThreadIdOrderByCreatedAtAsc(String threadId);
    long countByThreadId(String threadId);
    void deleteByThreadIdIn(List<String> threadIds);
}
