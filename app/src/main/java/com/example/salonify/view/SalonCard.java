package com.example.salonify.view;

import com.example.salonify.entity.Salon;

/** 一覧表示のサロンカード用の view model。 */
public record SalonCard(Salon salon, long memberCount, int planCount, Integer minPrice) {
    public String id() { return salon.getId(); }
    public String name() { return salon.getName(); }
    public String tagline() { return salon.getTagline(); }
    public String category() { return salon.getCategory(); }
    public String thumbUrl() { return salon.getThumbUrl(); }
}
