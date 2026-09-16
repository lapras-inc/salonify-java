package com.example.salonify.repository;

import com.example.salonify.entity.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, String> {
    Optional<Reaction> findByThreadIdAndUserIdAndKind(String threadId, String userId, String kind);
    long countByThreadId(String threadId);
    void deleteByThreadIdIn(List<String> threadIds);
}
