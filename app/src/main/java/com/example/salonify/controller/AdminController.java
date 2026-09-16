package com.example.salonify.controller;

import com.example.salonify.entity.*;
import com.example.salonify.entity.Thread;
import com.example.salonify.repository.*;
import com.example.salonify.service.RevenueService;
import com.example.salonify.service.SalonDeletionService;
import com.example.salonify.support.Auth;
import com.example.salonify.view.AdminSalonRow;
import com.example.salonify.view.ReportRow;
import com.example.salonify.web.NotFoundException;
import com.example.salonify.web.Redirects;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
public class AdminController {

    private static final int PAGE_SIZE = 20;

    private final UserRepository users;
    private final SalonRepository salons;
    private final MembershipRepository memberships;
    private final ReportRepository reports;
    private final ThreadRepository threads;
    private final Auth auth;
    private final SalonDeletionService salonDeletion;
    private final RevenueService revenueService;

    public AdminController(UserRepository users, SalonRepository salons, MembershipRepository memberships,
                          ReportRepository reports, ThreadRepository threads,
                          Auth auth, SalonDeletionService salonDeletion, RevenueService revenueService) {
        this.users = users;
        this.salons = salons;
        this.memberships = memberships;
        this.reports = reports;
        this.threads = threads;
        this.auth = auth;
        this.salonDeletion = salonDeletion;
        this.revenueService = revenueService;
    }

    @GetMapping("/admin")
    public String dashboard(HttpServletRequest request, Model model) {
        auth.requireAdmin(request);
        long totalRevenue = revenueService.totalPaidRevenue();
        model.addAttribute("userCount", users.count());
        model.addAttribute("salonCount", salons.count());
        model.addAttribute("activeMembershipCount", memberships.countByStatus(MembershipStatus.ACTIVE));
        model.addAttribute("totalRevenue", totalRevenue);
        return "admin/dashboard";
    }

    @GetMapping("/admin/users")
    public String usersList(@RequestParam(required = false) String q,
                            @RequestParam(defaultValue = "1") int page,
                            HttpServletRequest request, Model model) {
        auth.requireAdmin(request);
        int p = Math.max(1, page);
        PageRequest pr = PageRequest.of(p - 1, PAGE_SIZE);
        Page<User> pg = (q != null && !q.isEmpty())
                ? users.findByEmailContainingIgnoreCaseOrderByCreatedAtDesc(q, pr)
                : users.findAllByOrderByCreatedAtDesc(pr);
        model.addAttribute("users", pg.getContent());
        model.addAttribute("q", q);
        model.addAttribute("page", p);
        model.addAttribute("totalPages", pg.getTotalPages());
        return "admin/users";
    }

    @GetMapping("/admin/salons")
    public String salonsList(@RequestParam(defaultValue = "1") int page,
                             HttpServletRequest request, Model model) {
        auth.requireAdmin(request);
        int p = Math.max(1, page);
        Page<Salon> pg = salons.findAllByOrderByCreatedAtDesc(PageRequest.of(p - 1, PAGE_SIZE));
        List<AdminSalonRow> rows = new ArrayList<>();
        for (Salon s : pg.getContent()) {
            rows.add(new AdminSalonRow(
                    s,
                    users.findById(s.getOwnerId()).map(User::getEmail).orElse("?"),
                    memberships.countBySalonIdAndStatus(s.getId(), MembershipStatus.ACTIVE)));
        }
        model.addAttribute("rows", rows);
        model.addAttribute("page", p);
        model.addAttribute("totalPages", pg.getTotalPages());
        return "admin/salons";
    }

    @GetMapping("/admin/reports")
    public String reportsList(HttpServletRequest request, Model model) {
        auth.requireAdmin(request);
        List<ReportRow> rows = new ArrayList<>();
        for (Report r : reports.findAllByOrderByCreatedAtDesc()) {
            Thread t = r.getThreadId() != null ? threads.findById(r.getThreadId()).orElse(null) : null;
            rows.add(new ReportRow(
                    r,
                    users.findById(r.getReporterId()).map(User::getDisplayName).orElse("?"),
                    t));
        }
        model.addAttribute("rows", rows);
        return "admin/reports";
    }

    // ---------- アクション ----------
    /**
     * ユーザーを凍結する。passwordHashを非bcrypt値("suspended")にして以後のログインのみを止める。
     * セッションはステートレスなJWTで発行時のuidしか見ないため、凍結時点で発行済みのcookie
     * (最長30日有効)は失効しない。即時のアクセス遮断が必要な場合は別途対応が必要。
     */
    @PostMapping("/api/admin/users/{id}/suspend")
    @Transactional
    public ResponseEntity<Void> suspend(@PathVariable String id, HttpServletRequest request) {
        auth.requireAdmin(request);
        User u = users.findById(id).orElseThrow(NotFoundException::new);
        u.setPasswordHash("suspended");
        users.save(u);
        return Redirects.see("/admin/users?msg=user-suspended");
    }

    @PostMapping("/api/admin/users/{id}/toggle-admin")
    @Transactional
    public ResponseEntity<Void> toggleAdmin(@PathVariable String id, HttpServletRequest request) {
        auth.requireAdmin(request);
        User u = users.findById(id).orElseThrow(NotFoundException::new);
        u.setAdmin(!u.isAdmin());
        users.save(u);
        return Redirects.see("/admin/users?msg=admin-toggled");
    }

    @PostMapping("/api/admin/salons/{id}/delete")
    @Transactional
    public ResponseEntity<Void> deleteSalon(@PathVariable String id, HttpServletRequest request) {
        auth.requireAdmin(request);
        if (salons.findById(id).isEmpty()) throw new NotFoundException();
        salonDeletion.deleteSalon(id);
        return Redirects.see("/admin/salons?msg=salon-deleted");
    }

    @PostMapping("/api/admin/reports/{id}/resolve")
    @Transactional
    public ResponseEntity<Void> resolveReport(@PathVariable String id, HttpServletRequest request) {
        auth.requireAdmin(request);
        Report r = reports.findById(id).orElseThrow(NotFoundException::new);
        r.setResolved(true);
        reports.save(r);
        return Redirects.see("/admin/reports?msg=report-resolved");
    }
}
