package com.example.salonify.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comments", indexes = {
        @Index(columnList = "threadId,createdAt"),
        @Index(columnList = "parentId")
})
public class Comment {
    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String threadId;

    @Column(nullable = false)
    private String authorId;

    private String parentId; // 自己参照、ネストは1階層のみ

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
