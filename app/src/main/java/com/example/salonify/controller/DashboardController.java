package com.example.salonify.controller;

import com.example.salonify.entity.Membership;
import com.example.salonify.entity.MembershipStatus;
import com.example.salonify.entity.Plan;
import com.example.salonify.entity.Salon;
import com.example.salonify.entity.User;
import com.example.salonify.repository.MembershipRepository;
import com.example.salonify.repository.PlanRepository;
import com.example.salonify.repository.SalonRepository;
import com.example.salonify.support.Auth;
import com.example.salonify.view.JoinedSalonRow;
import com.example.salonify.view.OwnedSalonRow;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class DashboardController {

    private final SalonRepository salons;
    private final PlanRepository plans;
    private final MembershipRepository memberships;
    private final Auth auth;

    public DashboardController(SalonRepository salons, PlanRepository plans,
                              MembershipRepository memberships, Auth auth) {
        this.salons = salons;
        this.plans = plans;
        this.memberships = memberships;
        this.auth = auth;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {
        User me = auth.requireUser(request);

        // 参加中のサロン（有効なメンバーシップ、新しい順）
        List<JoinedSalonRow> joined = new ArrayList<>();
        for (Membership ms : memberships.findByUserIdAndStatusOrderByJoinedAtDesc(me.getId(), MembershipStatus.ACTIVE)) {
            Salon salon = salons.findById(ms.getSalonId()).orElse(null);
            Plan plan = plans.findById(ms.getPlanId()).orElse(null);
            joined.add(new JoinedSalonRow(salon, plan));
        }

        // 所有しているサロンと有効なメンバー数
        List<OwnedSalonRow> owned = new ArrayList<>();
        for (Salon salon : salons.findByOwnerIdOrderByCreatedAtDesc(me.getId())) {
            owned.add(new OwnedSalonRow(salon, memberships.countBySalonIdAndStatus(salon.getId(), MembershipStatus.ACTIVE)));
        }

        model.addAttribute("joined", joined);
        model.addAttribute("owned", owned);
        return "dashboard";
    }
}
