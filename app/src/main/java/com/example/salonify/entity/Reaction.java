package com.example.salonify.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"threadId", "userId", "kind"}))
public class Reaction {
    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String threadId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String kind = "like";

    public String getId() { return id; }
    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
}
