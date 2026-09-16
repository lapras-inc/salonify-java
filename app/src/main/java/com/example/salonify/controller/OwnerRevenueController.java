package com.example.salonify.controller;

import com.example.salonify.service.RevenueService;
import com.example.salonify.support.Auth;
import com.example.salonify.web.Redirects;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class OwnerRevenueController {

    // 振込を許可する最低純利益額(円)。少額振込の手数料倒れを防ぐ下限(値は暫定)。振込処理自体はモック(payout参照)。
    private static final int MIN_PAYOUT = 3000;

    private final Auth auth;
    private final RevenueService revenueService;

    public OwnerRevenueController(Auth auth, RevenueService revenueService) {
        this.auth = auth;
        this.revenueService = revenueService;
    }

    @GetMapping("/owner/salons/{id}/revenue")
    public String revenue(@PathVariable String id, HttpServletRequest request, Model model) {
        Auth.OwnerCtx ctx = auth.requireSalonOwner(request, id);

        RevenueService.RevenueSummary summary = revenueService.revenueFor(id);

        model.addAttribute("salon", ctx.salon());
        model.addAttribute("total", summary.total());
        model.addAttribute("fee", summary.fee());
        model.addAttribute("net", summary.net());
        model.addAttribute("canPayout", summary.net() >= MIN_PAYOUT);
        model.addAttribute("rows", summary.rows());
        return "owner/revenue";
    }

    @PostMapping("/api/owner/salons/{id}/payout")
    public ResponseEntity<Void> payout(@PathVariable String id, HttpServletRequest request) {
        auth.requireSalonOwner(request, id);
        // モック: 実際の振込は行わない。
        return Redirects.see("/owner/salons/" + id + "/revenue?msg=payout-requested");
    }
}
