package com.example.salonify.service;

import com.example.salonify.entity.Invite;
import com.example.salonify.repository.InviteRepository;
import com.example.salonify.support.Tokens;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class InviteService {

    private final InviteRepository invites;
    private final Tokens tokens;

    public InviteService(InviteRepository invites, Tokens tokens) {
        this.invites = invites;
        this.tokens = tokens;
    }

    /** 招待コードをサロンに対して検証する: 存在し、サロンが一致し、無効化されておらず、期限切れでなく、maxUses未満であること。 */
    public Invite findValid(String salonId, String code) {
        if (code == null || code.isEmpty()) return null;
        Invite inv = invites.findByCode(code).orElse(null);
        if (inv == null) return null;
        if (!inv.getSalonId().equals(salonId)) return null;
        if (inv.isDisabled()) return null;
        if (inv.getExpiresAt() != null && inv.getExpiresAt().isBefore(Instant.now())) return null;
        if (inv.getMaxUses() > 0 && inv.getUses() >= inv.getMaxUses()) return null;
        return inv;
    }

    public boolean isValid(String salonId, String code) {
        return findValid(salonId, code) != null;
    }

    /** 招待制への切り替え時、招待が1つも存在しなければ無制限の招待を自動生成する。 */
    public void ensureDefaultInvite(String salonId) {
        if (invites.existsBySalonId(salonId)) return;
        Invite inv = new Invite();
        inv.setSalonId(salonId);
        inv.setCode(tokens.inviteCode());
        inv.setMaxUses(0);
        invites.save(inv);
    }
}
