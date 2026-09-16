package com.example.salonify.controller;

import com.example.salonify.entity.*;
import com.example.salonify.repository.*;
import com.example.salonify.support.Auth;
import com.example.salonify.support.Forms;
import com.example.salonify.view.BillingRow;
import com.example.salonify.web.Redirects;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class AccountController {

    private final UserRepository users;
    private final SalonRepository salons;
    private final PlanRepository plans;
    private final MembershipRepository memberships;
    private final InvoiceRepository invoices;
    private final Auth auth;

    public AccountController(UserRepository users, SalonRepository salons, PlanRepository plans,
                             MembershipRepository memberships, InvoiceRepository invoices, Auth auth) {
        this.users = users;
        this.salons = salons;
        this.plans = plans;
        this.memberships = memberships;
        this.invoices = invoices;
        this.auth = auth;
    }

    @GetMapping("/account")
    public String account(HttpServletRequest request, Model model) {
        auth.requireUser(request);
        return "account/account";
    }

    @PostMapping("/api/account/update")
    @Transactional
    public ResponseEntity<Void> update(HttpServletRequest request,
                                       @RequestParam(defaultValue = "") String displayName,
                                       @RequestParam(required = false) String bio,
                                       @RequestParam(required = false) String avatarUrl) {
        User me = auth.currentUser(request);
        if (me == null) return Redirects.see("/login");
        me.setDisplayName(Forms.slice(displayName, 40));
        me.setBio(bio);
        me.setAvatarUrl(Forms.nullIfBlank(avatarUrl));
        users.save(me);
        return Redirects.see("/account?msg=account-updated");
    }

    @PostMapping("/api/account/delete")
    @Transactional
    public ResponseEntity<Void> delete(HttpServletRequest request, HttpServletResponse response) {
        User me = auth.currentUser(request);
        if (me == null) return Redirects.see("/login");

        // 全てのメンバーシップを解約し、アカウント情報を無効化する（FK整合性を保ったままの論理削除）
        for (Membership ms : memberships.findByUserId(me.getId())) {
            // 注意: DB上のステータスをCANCELLEDにするだけで、Stripe決済が有効でもStripe側のサブスクは
            // 解約されない(Webhook未実装)。Stripe課金ユーザーの実解約には別途Stripe側の操作が必要。
            ms.setStatus(MembershipStatus.CANCELLED);
            memberships.save(ms);
        }
        me.setEmail("deleted_" + me.getId() + "@example.invalid");
        me.setPasswordHash("deleted");
        me.setDisplayName("(退会したユーザー)");
        users.save(me);

        auth.logout(response);
        return Redirects.see("/?msg=account-deleted");
    }

    @GetMapping("/account/billing")
    public String billing(HttpServletRequest request, Model model) {
        User me = auth.requireUser(request);
        List<Membership> myMemberships = memberships.findByUserId(me.getId());

        // 表示用の行を組み立てる: サロン名、プラン名、ステータス、請求情報
        List<BillingRow> rows = new ArrayList<>();
        for (Membership ms : myMemberships) {
            Salon salon = salons.findById(ms.getSalonId()).orElse(null);
            Plan plan = plans.findById(ms.getPlanId()).orElse(null);
            rows.add(new BillingRow(ms, salon, plan, invoices.findByMembershipIdOrderByCreatedAtDesc(ms.getId())));
        }
        model.addAttribute("rows", rows);
        return "account/billing";
    }
}
