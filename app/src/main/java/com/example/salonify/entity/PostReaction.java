package com.example.salonify.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "post_reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"postId", "userId"}),
        indexes = @Index(columnList = "postId"))
public class PostReaction {
    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String postId;

    @Column(nullable = false)
    private String userId;

    public String getId() { return id; }
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
