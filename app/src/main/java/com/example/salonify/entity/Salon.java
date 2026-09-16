package com.example.salonify.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "salons", indexes = {
        @Index(columnList = "ownerId"),
        @Index(columnList = "visibility,createdAt")
})
public class Salon {
    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String ownerId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String tagline;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    private String coverUrl;
    private String thumbUrl;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String visibility = SalonVisibility.PUBLIC; // public | invite (private = レガシー)

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getThumbUrl() { return thumbUrl; }
    public void setThumbUrl(String thumbUrl) { this.thumbUrl = thumbUrl; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public boolean isPublic() { return SalonVisibility.PUBLIC.equals(visibility); }
    public boolean isInviteOnly() { return SalonVisibility.INVITE.equals(visibility); }
    public boolean isPrivate() { return SalonVisibility.PRIVATE.equals(visibility); }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
