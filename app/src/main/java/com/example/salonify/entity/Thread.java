package com.example.salonify.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "threads", indexes = @Index(columnList = "salonId,createdAt"))
public class Thread {
    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String salonId;

    @Column(nullable = false)
    private String authorId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public String getSalonId() { return salonId; }
    public void setSalonId(String salonId) { this.salonId = salonId; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
