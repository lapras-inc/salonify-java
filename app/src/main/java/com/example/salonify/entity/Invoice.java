package com.example.salonify.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invoices", indexes = @Index(columnList = "membershipId"))
public class Invoice {
    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String membershipId;

    @Column(nullable = false)
    private int amountJpy;

    @Column(nullable = false)
    private String status; // 現状 "paid" のみが実際にセットされる。failed/refunded(決済失敗・返金)のフローは未実装。

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public String getMembershipId() { return membershipId; }
    public void setMembershipId(String membershipId) { this.membershipId = membershipId; }
    public int getAmountJpy() { return amountJpy; }
    public void setAmountJpy(int amountJpy) { this.amountJpy = amountJpy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
