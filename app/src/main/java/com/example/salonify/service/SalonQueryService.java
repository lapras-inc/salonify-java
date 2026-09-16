package com.example.salonify.service;

import com.example.salonify.entity.MembershipStatus;
import com.example.salonify.entity.Plan;
import com.example.salonify.entity.Salon;
import com.example.salonify.repository.MembershipRepository;
import com.example.salonify.repository.PlanRepository;
import com.example.salonify.view.SalonCard;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SalonQueryService {

    public static final List<String> CATEGORIES =
            List.of("ビジネス", "趣味", "アート", "テクノロジー", "教育", "その他");

    private final PlanRepository plans;
    private final MembershipRepository memberships;

    public SalonQueryService(PlanRepository plans, MembershipRepository memberships) {
        this.plans = plans;
        this.memberships = memberships;
    }

    public List<SalonCard> toCards(List<Salon> salons) {
        List<SalonCard> cards = new ArrayList<>();
        for (Salon s : salons) {
            List<Plan> p = plans.findBySalonIdOrderByPriceJpyAsc(s.getId());
            long members = memberships.countBySalonIdAndStatus(s.getId(), MembershipStatus.ACTIVE);
            Integer minPrice = p.stream().map(Plan::getPriceJpy).min(Comparator.naturalOrder()).orElse(null);
            cards.add(new SalonCard(s, members, p.size(), minPrice));
        }
        return cards;
    }

    /** カードのコピーをアクティブメンバー数の降順でソートする(「人気順」ソート用)。 */
    public List<SalonCard> sortByPopular(List<SalonCard> cards) {
        List<SalonCard> copy = new ArrayList<>(cards);
        copy.sort(Comparator.comparingLong(SalonCard::memberCount).reversed());
        return copy;
    }
}
