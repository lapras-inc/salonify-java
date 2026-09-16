package com.example.salonify.controller;

import com.example.salonify.entity.Plan;
import com.example.salonify.entity.Salon;
import com.example.salonify.entity.SalonVisibility;
import com.example.salonify.repository.*;
import com.example.salonify.service.InviteService;
import com.example.salonify.service.RevenueService;
import com.example.salonify.service.SalonQueryService;
import com.example.salonify.support.Auth;
import com.example.salonify.support.Forms;
import com.example.salonify.web.NotFoundException;
import com.example.salonify.web.Redirects;
import com.example.salonify.web.Urls;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class OwnerSalonController {

    private final SalonRepository salons;
    private final PlanRepository plans;
    private final PostRepository posts;
    private final ThreadRepository threads;
    private final InviteRepository invites;
    private final Auth auth;
    private final InviteService inviteService;
    private final RevenueService revenueService;

    public OwnerSalonController(SalonRepository salons, PlanRepository plans,
                                PostRepository posts, ThreadRepository threads, InviteRepository invites,
                                Auth auth, InviteService inviteService, RevenueService revenueService) {
        this.salons = salons;
        this.plans = plans;
        this.posts = posts;
        this.threads = threads;
        this.invites = invites;
        this.auth = auth;
        this.inviteService = inviteService;
        this.revenueService = revenueService;
    }

    // ---------- 新規作成 ----------
    @GetMapping("/owner/salons/new")
    public String newSalon(HttpServletRequest request, Model model) {
        auth.requireUser(request);
        model.addAttribute("categories", SalonQueryService.CATEGORIES);
        return "owner/new";
    }

    @PostMapping("/api/owner/salons/create")
    @Transactional
    public ResponseEntity<Void> create(HttpServletRequest request,
                                       @RequestParam(defaultValue = "") String name,
                                       @RequestParam(defaultValue = "") String tagline,
                                       @RequestParam(defaultValue = "") String description,
                                       @RequestParam(defaultValue = "") String category,
                                       @RequestParam(defaultValue = SalonVisibility.PUBLIC) String visibility,
                                       @RequestParam(required = false) String coverUrl,
                                       @RequestParam(required = false) String thumbUrl,
                                       @RequestParam(required = false) String planName,
                                       @RequestParam(required = false) String planPrice,
                                       @RequestParam(required = false) String planTrial,
                                       @RequestParam(required = false) String planDiscount) {
        var user = auth.requireUser(request);

        Salon salon = new Salon();
        salon.setOwnerId(user.getId());
        salon.setName(Forms.slice(name, 80));
        salon.setTagline(Forms.slice(tagline, 140));
        salon.setDescription(description);
        salon.setCategory(category);
        salon.setVisibility(SalonVisibility.normalize(visibility));
        salon.setCoverUrl(Forms.nullIfBlank(coverUrl));
        salon.setThumbUrl(Forms.nullIfBlank(thumbUrl));
        salons.save(salon);

        Plan plan = new Plan();
        plan.setSalonId(salon.getId());
        plan.setName(Forms.orDefault(planName, "スタンダード"));
        plan.setPriceJpy(Forms.clamp(Forms.parseInt(planPrice, 980), 0, 100000));
        plan.setTrialDays(Forms.clamp(Forms.parseInt(planTrial, 0), 0, 30));
        plan.setIntroDiscount(Math.max(0, Forms.parseInt(planDiscount, 0)));
        plans.save(plan);

        return Redirects.see("/owner/salons/" + salon.getId() + "/dashboard?msg=salon-created");
    }

    // ---------- ダッシュボード ----------
    @GetMapping("/owner/salons/{id}/dashboard")
    public String dashboard(@PathVariable String id, HttpServletRequest request, Model model) {
        Auth.OwnerCtx ctx = auth.requireSalonOwner(request, id);
        Salon salon = ctx.salon();
        RevenueService.MonthlyEstimate estimate = revenueService.monthlyEstimate(id);

        model.addAttribute("salon", salon);
        model.addAttribute("plans", estimate.plans());
        model.addAttribute("perPlanCount", estimate.perPlanCount());
        model.addAttribute("monthlyRevenue", estimate.monthlyRevenue());
        model.addAttribute("activeCount", estimate.activeCount());
        model.addAttribute("postCount", posts.countBySalonId(id));
        model.addAttribute("threadCount", threads.countBySalonId(id));
        if (salon.isInviteOnly()) {
            model.addAttribute("invites", invites.findBySalonIdOrderByCreatedAtDesc(id));
            model.addAttribute("origin", Urls.origin(request));
        }
        return "owner/dashboard";
    }

    // ---------- 編集 ----------
    @GetMapping("/owner/salons/{id}/edit")
    public String edit(@PathVariable String id, HttpServletRequest request, Model model) {
        Auth.OwnerCtx ctx = auth.requireSalonOwner(request, id);
        Salon salon = ctx.salon();
        model.addAttribute("salon", salon);
        model.addAttribute("categories", SalonQueryService.CATEGORIES);
        model.addAttribute("plans", plans.findBySalonIdOrderByPriceJpyAsc(id));
        if (salon.isInviteOnly()) {
            model.addAttribute("invites", invites.findBySalonIdOrderByCreatedAtDesc(id));
            model.addAttribute("origin", Urls.origin(request));
        }
        return "owner/edit";
    }

    @PostMapping("/api/owner/salons/{id}/update")
    @Transactional
    public ResponseEntity<Void> update(@PathVariable String id, HttpServletRequest request,
                                       @RequestParam(defaultValue = "") String name,
                                       @RequestParam(defaultValue = "") String tagline,
                                       @RequestParam(defaultValue = "") String description,
                                       @RequestParam(defaultValue = "") String category,
                                       @RequestParam(defaultValue = SalonVisibility.PUBLIC) String visibility,
                                       @RequestParam(required = false) String coverUrl,
                                       @RequestParam(required = false) String thumbUrl) {
        Auth.OwnerCtx ctx = auth.requireSalonOwner(request, id);
        Salon salon = ctx.salon();
        String normalizedVisibility = SalonVisibility.normalize(visibility);
        salon.setName(Forms.slice(name, 80));
        salon.setTagline(Forms.slice(tagline, 140));
        salon.setDescription(description);
        salon.setCategory(category);
        salon.setVisibility(normalizedVisibility);
        salon.setCoverUrl(Forms.nullIfBlank(coverUrl));
        salon.setThumbUrl(Forms.nullIfBlank(thumbUrl));
        salons.save(salon);

        // 招待制に切り替えた際、招待コードが存在しなければ無制限の招待を自動生成する。
        if (SalonVisibility.INVITE.equals(normalizedVisibility)) {
            inviteService.ensureDefaultInvite(id);
        }

        return Redirects.see("/owner/salons/" + id + "/edit?msg=salon-updated");
    }

    // ---------- プラン ----------
    @PostMapping("/api/owner/salons/{id}/plan/create")
    @Transactional
    public ResponseEntity<Void> planCreate(@PathVariable String id, HttpServletRequest request,
                                           @RequestParam(defaultValue = "") String name,
                                           @RequestParam(required = false) String priceJpy,
                                           @RequestParam(required = false) String trialDays,
                                           @RequestParam(required = false) String introDiscount,
                                           @RequestParam(required = false) String description) {
        auth.requireSalonOwner(request, id);
        Plan plan = new Plan();
        plan.setSalonId(id);
        plan.setName(name);
        plan.setPriceJpy(Forms.clamp(Forms.parseInt(priceJpy, 0), 0, 100000));
        plan.setTrialDays(Forms.clamp(Forms.parseInt(trialDays, 0), 0, 30));
        plan.setIntroDiscount(Math.max(0, Forms.parseInt(introDiscount, 0)));
        plan.setDescription(Forms.nullIfBlank(description));
        plans.save(plan);
        return Redirects.see("/owner/salons/" + id + "/edit?msg=plan-added");
    }

    @PostMapping("/api/owner/salons/{id}/plan/update")
    @Transactional
    public ResponseEntity<Void> planUpdate(@PathVariable String id, HttpServletRequest request,
                                           @RequestParam String planId,
                                           @RequestParam(defaultValue = "") String name,
                                           @RequestParam(required = false) String priceJpy,
                                           @RequestParam(required = false) String trialDays,
                                           @RequestParam(required = false) String introDiscount,
                                           @RequestParam(required = false) String description) {
        auth.requireSalonOwner(request, id);
        Plan plan = plans.findById(planId).orElse(null);
        if (plan == null || !plan.getSalonId().equals(id)) {
            return Redirects.see("/owner/salons/" + id + "/edit");
        }
        plan.setName(name);
        plan.setPriceJpy(Forms.clamp(Forms.parseInt(priceJpy, 0), 0, 100000));
        plan.setTrialDays(Forms.clamp(Forms.parseInt(trialDays, 0), 0, 30));
        plan.setIntroDiscount(Math.max(0, Forms.parseInt(introDiscount, 0)));
        plan.setDescription(Forms.nullIfBlank(description));
        plans.save(plan);
        return Redirects.see("/owner/salons/" + id + "/edit?msg=plan-updated");
    }
}
