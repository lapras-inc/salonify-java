package com.example.salonify.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invites", indexes = @Index(columnList = "salonId"))
public class Invite {
    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String salonId;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String label = "";

    @Column(nullable = false)
    private int maxUses = 0; // 0 = 無制限

    @Column(nullable = false)
    private int uses = 0;

    @Column(nullable = false)
    private boolean disabled = false;

    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public String getSalonId() { return salonId; }
    public void setSalonId(String salonId) { this.salonId = salonId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public int getMaxUses() { return maxUses; }
    public void setMaxUses(int maxUses) { this.maxUses = maxUses; }
    public int getUses() { return uses; }
    public void setUses(int uses) { this.uses = uses; }
    public boolean isDisabled() { return disabled; }
    public void setDisabled(boolean disabled) { this.disabled = disabled; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
