package com.example.salonify.controller;

import com.example.salonify.entity.Invite;
import com.example.salonify.repository.InviteRepository;
import com.example.salonify.support.Auth;
import com.example.salonify.support.Forms;
import com.example.salonify.support.Tokens;
import com.example.salonify.web.Redirects;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Controller
public class OwnerInviteController {

    private final InviteRepository invites;
    private final Auth auth;
    private final Tokens tokens;

    public OwnerInviteController(InviteRepository invites, Auth auth, Tokens tokens) {
        this.invites = invites;
        this.auth = auth;
        this.tokens = tokens;
    }

    @PostMapping("/api/owner/salons/{id}/invite/create")
    @Transactional
    public ResponseEntity<Void> create(@PathVariable String id, HttpServletRequest request,
                                       @RequestParam(defaultValue = "") String label,
                                       @RequestParam(required = false) String maxUses,
                                       @RequestParam(required = false) String expDays,
                                       @RequestParam(required = false) String redirect) {
        auth.requireSalonOwner(request, id);
        int max = Math.max(0, Forms.parseInt(maxUses, 0));
        int days = Math.max(0, Forms.parseInt(expDays, 0));

        Invite inv = new Invite();
        inv.setSalonId(id);
        inv.setCode(tokens.inviteCode());
        inv.setLabel(Forms.slice(label, 40));
        inv.setMaxUses(max);
        inv.setExpiresAt(days > 0 ? Instant.now().plus(days, ChronoUnit.DAYS) : null);
        invites.save(inv);

        if (redirect != null && redirect.startsWith("/")) {
            String sep = redirect.contains("?") ? "&" : "?";
            return Redirects.see(redirect + sep + "msg=invite-created");
        }
        return Redirects.see("/owner/salons/" + id + "/edit?msg=invite-created");
    }

    @PostMapping("/api/owner/salons/{id}/invite/disable")
    @Transactional
    public ResponseEntity<Void> disable(@PathVariable String id, HttpServletRequest request,
                                        @RequestParam String inviteId) {
        auth.requireSalonOwner(request, id);
        Invite inv = invites.findById(inviteId).orElse(null);
        if (inv != null && inv.getSalonId().equals(id)) {
            inv.setDisabled(true);
            invites.save(inv);
        }
        return Redirects.see("/owner/salons/" + id + "/edit?msg=invite-disabled");
    }
}
