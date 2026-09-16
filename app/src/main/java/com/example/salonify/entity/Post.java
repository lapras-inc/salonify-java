package com.example.salonify.entity;

import com.example.salonify.support.PostAccess;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "posts", indexes = @Index(columnList = "salonId,createdAt"))
public class Post {
    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String salonId;

    @Column(nullable = false)
    private String authorId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String bodyHtml;

    @Column(columnDefinition = "text")
    private String bodyMarkdown;

    private String videoUrl;

    @Column(nullable = false)
    private String visibility = PostAccess.ALL; // all | plan:<planId>

    @Column(nullable = false)
    private boolean pinned = false;

    @Column(nullable = false)
    private boolean draft = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public String getSalonId() { return salonId; }
    public void setSalonId(String salonId) { this.salonId = salonId; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBodyHtml() { return bodyHtml; }
    public void setBodyHtml(String bodyHtml) { this.bodyHtml = bodyHtml; }
    public String getBodyMarkdown() { return bodyMarkdown; }
    public void setBodyMarkdown(String bodyMarkdown) { this.bodyMarkdown = bodyMarkdown; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public boolean isDraft() { return draft; }
    public void setDraft(boolean draft) { this.draft = draft; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
