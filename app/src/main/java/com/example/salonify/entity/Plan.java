package com.example.salonify.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "plans", indexes = @Index(columnList = "salonId"))
public class Plan {
    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String salonId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int priceJpy;

    @Column(nullable = false)
    private int trialDays = 0;

    @Column(nullable = false)
    private int introDiscount = 0; // 初月の割引額(円)

    @Column(columnDefinition = "text")
    private String description;

    private String stripePriceId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSalonId() { return salonId; }
    public void setSalonId(String salonId) { this.salonId = salonId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getPriceJpy() { return priceJpy; }
    public void setPriceJpy(int priceJpy) { this.priceJpy = priceJpy; }
    public int getTrialDays() { return trialDays; }
    public void setTrialDays(int trialDays) { this.trialDays = trialDays; }
    public int getIntroDiscount() { return introDiscount; }
    public void setIntroDiscount(int introDiscount) { this.introDiscount = introDiscount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStripePriceId() { return stripePriceId; }
    public void setStripePriceId(String stripePriceId) { this.stripePriceId = stripePriceId; }
}
