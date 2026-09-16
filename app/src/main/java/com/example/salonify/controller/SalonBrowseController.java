package com.example.salonify.controller;

import com.example.salonify.entity.*;
import com.example.salonify.repository.*;
import com.example.salonify.service.InviteService;
import com.example.salonify.service.JoinService;
import com.example.salonify.service.SalonQueryService;
import com.example.salonify.support.Auth;
import com.example.salonify.support.Sanitizer;
import com.example.salonify.view.SalonCard;
import com.example.salonify.web.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SalonBrowseController {

    private final SalonRepository salons;
    private final PlanRepository plans;
    private final UserRepository users;
    private final MembershipRepository memberships;
    private final InviteService inviteService;
    private final JoinService joinService;
    private final SalonQueryService salonQuery;
    private final Sanitizer sanitizer;
    private final Auth auth;

    public SalonBrowseController(SalonRepository salons, PlanRepository plans, UserRepository users,
                                 MembershipRepository memberships, InviteService inviteService,
                                 JoinService joinService, SalonQueryService salonQuery, Sanitizer sanitizer,
                                 Auth auth) {
        this.salons = salons;
        this.plans = plans;
        this.users = users;
        this.memberships = memberships;
        this.inviteService = inviteService;
        this.joinService = joinService;
        this.salonQuery = salonQuery;
        this.sanitizer = sanitizer;
        this.auth = auth;
    }

    @GetMapping("/salons")
    public String list(@RequestParam(required = false) String category,
                       @RequestParam(required = false) String sort, Model model) {
        List<Salon> list = (category != null && !category.isEmpty())
                ? salons.findByVisibilityAndCategoryOrderByCreatedAtDesc(SalonVisibility.PUBLIC, category)
                : salons.findByVisibilityOrderByCreatedAtDesc(SalonVisibility.PUBLIC);
        List<SalonCard> cards = salonQuery.toCards(list);
        if ("popular".equals(sort)) cards = salonQuery.sortByPopular(cards);
        model.addAttribute("cards", cards);
        model.addAttribute("categories", SalonQueryService.CATEGORIES);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("sort", sort);
        return "salon/list";
    }

    @GetMapping("/salons/{id}")
    public String detail(@PathVariable String id,
                         @RequestParam(required = false) String invite,
                         HttpServletRequest request, Model model) {
        Salon salon = salons.findById(id).orElseThrow(NotFoundException::new);
        User owner = users.findById(salon.getOwnerId()).orElse(null);
        List<Plan> planList = plans.findBySalonIdOrderByPriceJpyAsc(id);
        long activeCount = memberships.countBySalonIdAndStatus(id, MembershipStatus.ACTIVE);
        User me = auth.currentUser(request);

        boolean isOwner = me != null && salon.getOwnerId().equals(me.getId());
        Membership myMs = me != null ? memberships.findByUserIdAndSalonId(me.getId(), id).orElse(null) : null;
        boolean isActiveMember = myMs != null && myMs.isActive();
        String myActivePlanId = isActiveMember ? myMs.getPlanId() : null;

        // 招待による制限
        boolean inviteValid = inviteService.isValid(id, invite);
        boolean inviteAllowed = joinService.inviteAllowed(salon, isActiveMember, isOwner, invite);
        // 旧式の非公開サロン: オーナー・メンバー以外には詳細を非表示にする
        boolean privateLocked = salon.isPrivate() && !isOwner && !isActiveMember;

        model.addAttribute("salon", salon);
        model.addAttribute("owner", owner);
        model.addAttribute("plans", planList);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("descriptionHtml", sanitizer.renderMarkdown(salon.getDescription()));
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isActiveMember", isActiveMember);
        model.addAttribute("myActivePlanId", myActivePlanId);
        model.addAttribute("inviteAllowed", inviteAllowed);
        model.addAttribute("inviteCode", inviteValid ? invite : null);
        model.addAttribute("privateLocked", privateLocked);
        return "salon/detail";
    }
}
