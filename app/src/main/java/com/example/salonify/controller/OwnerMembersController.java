package com.example.salonify.controller;

import com.example.salonify.entity.Invoice;
import com.example.salonify.entity.Membership;
import com.example.salonify.entity.MembershipStatus;
import com.example.salonify.entity.Plan;
import com.example.salonify.entity.User;
import com.example.salonify.repository.InvoiceRepository;
import com.example.salonify.repository.MembershipRepository;
import com.example.salonify.repository.PlanRepository;
import com.example.salonify.repository.UserRepository;
import com.example.salonify.support.Auth;
import com.example.salonify.view.MemberRow;
import com.example.salonify.web.Redirects;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class OwnerMembersController {

    private final MembershipRepository memberships;
    private final UserRepository users;
    private final PlanRepository plans;
    private final InvoiceRepository invoices;
    private final Auth auth;

    public OwnerMembersController(MembershipRepository memberships, UserRepository users, PlanRepository plans,
                                  InvoiceRepository invoices, Auth auth) {
        this.memberships = memberships;
        this.users = users;
        this.plans = plans;
        this.invoices = invoices;
        this.auth = auth;
    }

    @GetMapping("/owner/salons/{id}/members")
    public String members(@PathVariable String id, HttpServletRequest request, Model model) {
        Auth.OwnerCtx ctx = auth.requireSalonOwner(request, id);
        List<MemberRow> rows = new ArrayList<>();
        for (Membership ms : memberships.findBySalonIdOrderByJoinedAtDesc(id)) {
            rows.add(new MemberRow(
                    ms,
                    users.findById(ms.getUserId()).orElse(null),
                    plans.findById(ms.getPlanId()).orElse(null),
                    invoices.findFirstByMembershipIdOrderByCreatedAtDesc(ms.getId()).orElse(null)));
        }
        model.addAttribute("salon", ctx.salon());
        model.addAttribute("rows", rows);
        model.addAttribute("plans", plans.findBySalonIdOrderByPriceJpyAsc(id));
        return "owner/members";
    }

    @PostMapping("/api/owner/salons/{id}/members/change-plan")
    @Transactional
    public ResponseEntity<Void> changePlan(@PathVariable String id, HttpServletRequest request,
                                           @RequestParam String membershipId, @RequestParam String planId) {
        auth.requireSalonOwner(request, id);
        Membership ms = memberships.findById(membershipId).orElse(null);
        Plan plan = plans.findById(planId).orElse(null);
        if (ms != null && ms.getSalonId().equals(id) && plan != null && plan.getSalonId().equals(id)) {
            ms.setPlanId(planId);
            memberships.save(ms);
        }
        return Redirects.see("/owner/salons/" + id + "/members?msg=member-plan-changed");
    }

    @PostMapping("/api/owner/salons/{id}/members/remove")
    @Transactional
    public ResponseEntity<Void> remove(@PathVariable String id, HttpServletRequest request,
                                       @RequestParam String membershipId) {
        auth.requireSalonOwner(request, id);
        Membership ms = memberships.findById(membershipId).orElse(null);
        if (ms != null && ms.getSalonId().equals(id)) {
            // 注意: DB上のステータス変更のみ。Stripe決済が有効でもStripe側のサブスクは解約されない(Webhook未実装)。
            ms.setStatus(MembershipStatus.CANCELLED);
            memberships.save(ms);
        }
        return Redirects.see("/owner/salons/" + id + "/members?msg=member-removed");
    }
}
