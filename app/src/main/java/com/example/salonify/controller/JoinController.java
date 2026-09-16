package com.example.salonify.controller;

import com.example.salonify.entity.*;
import com.example.salonify.repository.*;
import com.example.salonify.service.JoinService;
import com.example.salonify.service.StripeCheckoutService;
import com.example.salonify.support.Auth;
import com.example.salonify.web.NotFoundException;
import com.example.salonify.web.RedirectException;
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
public class JoinController {

    private final SalonRepository salons;
    private final PlanRepository plans;
    private final MembershipRepository memberships;
    private final JoinService joinService;
    private final StripeCheckoutService stripeCheckout;
    private final Auth auth;

    public JoinController(SalonRepository salons, PlanRepository plans, MembershipRepository memberships,
                          JoinService joinService, StripeCheckoutService stripeCheckout, Auth auth) {
        this.salons = salons;
        this.plans = plans;
        this.memberships = memberships;
        this.joinService = joinService;
        this.stripeCheckout = stripeCheckout;
        this.auth = auth;
    }

    // ---------- 参加確認ページ ----------
    @GetMapping("/salons/{id}/join")
    public String joinPage(@PathVariable String id,
                           @RequestParam(required = false) String plan,
                           @RequestParam(required = false) String invite,
                           HttpServletRequest request, Model model) {
        if (auth.currentUser(request) == null) {
            throw new RedirectToLogin("/salons/" + id + "/join");
        }
        Salon salon = salons.findById(id).orElseThrow(NotFoundException::new);
        User me = auth.currentUser(request);
        Membership existing = memberships.findByUserIdAndSalonId(me.getId(), id).orElse(null);
        boolean isActiveMember = existing != null && existing.isActive();
        boolean isOwner = salon.getOwnerId().equals(me.getId());

        if (!joinService.inviteAllowed(salon, isActiveMember, isOwner, invite)) {
            return "redirect:/salons/" + id;
        }

        List<Plan> planList = plans.findBySalonIdOrderByPriceJpyAsc(id);
        if (planList.isEmpty()) return "redirect:/salons/" + id;
        Plan selected = planList.stream().filter(p -> p.getId().equals(plan)).findFirst().orElse(planList.get(0));

        model.addAttribute("salon", salon);
        model.addAttribute("plan", selected);
        model.addAttribute("invite", invite);
        model.addAttribute("firstAmount", joinService.firstAmount(selected));
        return "salon/join";
    }

    // ---------- 参加アクション（モック決済） ----------
    @PostMapping("/api/join")
    public ResponseEntity<Void> join(@RequestParam String salonId,
                                     @RequestParam String planId,
                                     @RequestParam(required = false) String invite,
                                     HttpServletRequest request) {
        User me = auth.currentUser(request);
        if (me == null) return Redirects.see("/login");
        String baseUrl = Urls.origin(request);
        return Redirects.see(joinService.join(me, salonId, planId, invite, baseUrl));
    }

    // ---------- 参加確認（Stripe Checkout 成功時のリダイレクト） ----------
    @GetMapping("/api/join/success")
    public ResponseEntity<Void> joinSuccess(@RequestParam(name = "session_id", required = false) String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) return Redirects.see("/");

        StripeCheckoutService.JoinSession joined = stripeCheckout.retrieveJoinSession(sessionId);
        if (joined == null) return Redirects.see("/");

        return Redirects.see(joinService.confirmStripeJoin(
                joined.userId(), joined.salonId(), joined.planId(), joined.subscriptionId()));
    }

    // ---------- メンバーシップ解約 ----------
    @PostMapping("/api/membership/cancel")
    @Transactional
    public ResponseEntity<Void> cancel(@RequestParam String membershipId,
                                       @RequestParam(required = false) String redirect,
                                       HttpServletRequest request) {
        User me = auth.currentUser(request);
        if (me == null) return Redirects.see("/login");

        String dest = (redirect != null && redirect.startsWith("/")) ? redirect : "/account/billing";
        Membership ms = memberships.findById(membershipId).orElse(null);
        if (ms != null && ms.getUserId().equals(me.getId())) {
            // 注意: DB上のステータス変更のみ。Stripe決済が有効でもStripe側のサブスクは解約されない(Webhook未実装)。
            ms.setStatus(MembershipStatus.CANCELLED);
            memberships.save(ms);
        }
        String sep = dest.contains("?") ? "&" : "?";
        return Redirects.see(dest + sep + "msg=cancelled");
    }

    /** ローカル例外: next パラメータを保持したまま /login にリダイレクトする。 */
    static class RedirectToLogin extends RedirectException {
        RedirectToLogin(String next) {
            super("/login?next=" + java.net.URLEncoder.encode(next, java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
