package com.example.salonify.service;

import com.example.salonify.entity.Invite;
import com.example.salonify.entity.Invoice;
import com.example.salonify.entity.InvoiceStatus;
import com.example.salonify.entity.Membership;
import com.example.salonify.entity.MembershipStatus;
import com.example.salonify.entity.Plan;
import com.example.salonify.entity.Salon;
import com.example.salonify.entity.User;
import com.example.salonify.repository.InvoiceRepository;
import com.example.salonify.repository.InviteRepository;
import com.example.salonify.repository.MembershipRepository;
import com.example.salonify.repository.PlanRepository;
import com.example.salonify.repository.SalonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JoinService {

    private final SalonRepository salons;
    private final PlanRepository plans;
    private final MembershipRepository memberships;
    private final InvoiceRepository invoices;
    private final InviteRepository invites;
    private final InviteService inviteService;
    private final StripeCheckoutService stripeCheckout;

    public JoinService(SalonRepository salons, PlanRepository plans, MembershipRepository memberships,
                       InvoiceRepository invoices, InviteRepository invites, InviteService inviteService,
                       StripeCheckoutService stripeCheckout) {
        this.salons = salons;
        this.plans = plans;
        this.memberships = memberships;
        this.invoices = invoices;
        this.invites = invites;
        this.inviteService = inviteService;
        this.stripeCheckout = stripeCheckout;
    }

    /** プランの初回請求額: トライアル期間中は無料、それ以外は価格から初月割引を引いた額(下限0)。 */
    public int firstAmount(Plan plan) {
        return plan.getTrialDays() > 0 ? 0 : Math.max(0, plan.getPriceJpy() - plan.getIntroDiscount());
    }

    /** 現在の閲覧者が招待制サロンのゲートを通過できるかどうか。 */
    public boolean inviteAllowed(Salon salon, boolean isActiveMember, boolean isOwner, String inviteCode) {
        return !salon.isInviteOnly() || isActiveMember || isOwner
                || inviteService.isValid(salon.getId(), inviteCode);
    }

    /** サロンへの参加/再参加/プラン変更を行う。リダイレクト先のパス(またはStripe CheckoutのURL)を返す。 */
    @Transactional
    public String join(User me, String salonId, String planId, String inviteCode, String baseUrl) {
        Salon salon = salons.findById(salonId).orElse(null);
        if (salon == null) return "/";

        Membership existing = memberships.findByUserIdAndSalonId(me.getId(), salonId).orElse(null);
        boolean isActiveMember = existing != null && existing.isActive();

        // 招待によるゲーティング(参照実装と同様、有効な招待の場合はusesをインクリメントする)
        if (salon.isInviteOnly() && !isActiveMember) {
            Invite inv = inviteService.findValid(salonId, inviteCode);
            if (inv == null) return "/salons/" + salonId;
            inv.setUses(inv.getUses() + 1);
            invites.save(inv);
        }

        Plan plan = plans.findById(planId).orElse(null);
        if (plan == null || !plan.getSalonId().equals(salonId)) {
            return "/salons/" + salonId;
        }

        // 既に同じプランでアクティブ -> 何もしない
        if (isActiveMember && existing.getPlanId().equals(planId)) {
            return "/salons/" + salonId + "/home";
        }

        // 別のプランでアクティブ -> プラン変更
        // 注意: ここではDB上のplanIdを書き換えるだけで、Stripe決済が有効でもStripe側のサブスク/価格は
        // 更新しない。実際の請求額と表示額がズレうる(サブスク変更・継続課金Webhookは未実装)。
        if (isActiveMember) {
            existing.setPlanId(planId);
            memberships.save(existing);
            return "/salons/" + salonId + "/home?msg=plan-changed";
        }

        // 新規参加または再参加
        if (stripeCheckout.enabled()) {
            return stripeCheckout.createCheckoutUrl(me, salonId, plan, inviteCode, baseUrl);
        }

        // モック: 実際の請求は発生しない
        int days = plan.getTrialDays() > 0 ? plan.getTrialDays() : 30;
        Instant nextBill = Instant.now().plus(days, ChronoUnit.DAYS);
        int firstAmount = firstAmount(plan);

        activateMembership(existing, me.getId(), salonId, planId, nextBill, firstAmount, null);

        return "/salons/" + salonId + "/home?msg=joined";
    }

    /** Stripe Checkoutセッションを確認し、メンバーシップを有効化する(/api/join/successのリダイレクト先)。 */
    @Transactional
    public String confirmStripeJoin(String userId, String salonId, String planId, String subscriptionId) {
        Plan plan = plans.findById(planId).orElse(null);
        int days = (plan != null && plan.getTrialDays() > 0) ? plan.getTrialDays() : 30;
        Instant nextBill = Instant.now().plus(days, ChronoUnit.DAYS);
        int firstAmount = plan != null ? firstAmount(plan) : 0;

        Membership ms = memberships.findByUserIdAndSalonId(userId, salonId).orElse(null);
        activateMembership(ms, userId, salonId, planId, nextBill, firstAmount, subscriptionId);

        return "/salons/" + salonId + "/home?msg=joined";
    }

    /** 既存のメンバーシップがあれば再利用し、なければ新規作成して有効化する。請求が発生した場合は支払い済みインボイスを記録する。 */
    private void activateMembership(Membership existing, String userId, String salonId, String planId,
                                     Instant nextBill, int firstAmount, String subscriptionId) {
        Membership ms = existing != null ? existing : new Membership();
        ms.setUserId(userId);
        ms.setSalonId(salonId);
        ms.setPlanId(planId);
        ms.setStatus(MembershipStatus.ACTIVE);
        ms.setFailedCount(0);
        ms.setJoinedAt(Instant.now());
        ms.setNextBillAt(nextBill);
        if (subscriptionId != null) ms.setStripeSubscriptionId(subscriptionId);
        memberships.save(ms);

        if (firstAmount > 0) {
            Invoice inv = new Invoice();
            inv.setMembershipId(ms.getId());
            inv.setAmountJpy(firstAmount);
            inv.setStatus(InvoiceStatus.PAID);
            invoices.save(inv);
        }
    }
}
