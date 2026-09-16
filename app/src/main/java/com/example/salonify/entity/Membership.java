package com.example.salonify.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memberships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "salonId"}),
        indexes = {
                @Index(columnList = "salonId,status"),
                @Index(columnList = "userId,status")
        })
public class Membership {
    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String salonId;

    @Column(nullable = false)
    private String planId;

    private String stripeSubscriptionId;

    @Column(nullable = false)
    private String status = MembershipStatus.ACTIVE; // active | past_due | cancelled | suspended

    @Column(nullable = false)
    private Instant joinedAt = Instant.now();

    // 次回請求予定日時。現状これを読んで請求を実行するバッチ/@Scheduledジョブは無い(継続課金は未実装)。
    // そのためInvoiceは加入時の1回分しか記録されず、RevenueServiceの実績集計と月次見込みが乖離する。
    @Column(nullable = false)
    private Instant nextBillAt;

    @Column(nullable = false)
    private int failedCount = 0;

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSalonId() { return salonId; }
    public void setSalonId(String salonId) { this.salonId = salonId; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String stripeSubscriptionId) { this.stripeSubscriptionId = stripeSubscriptionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isActive() { return MembershipStatus.ACTIVE.equals(status); }
    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }
    public Instant getNextBillAt() { return nextBillAt; }
    public void setNextBillAt(Instant nextBillAt) { this.nextBillAt = nextBillAt; }
    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
}
