package com.example.salonify;

import com.example.salonify.entity.Invite;
import com.example.salonify.entity.Invoice;
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
import com.example.salonify.service.InviteService;
import com.example.salonify.service.JoinService;
import com.example.salonify.service.StripeCheckoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JoinServiceTest {

    @Mock private SalonRepository salons;
    @Mock private PlanRepository plans;
    @Mock private MembershipRepository memberships;
    @Mock private InvoiceRepository invoices;
    @Mock private InviteRepository invites;
    @Mock private InviteService inviteService;
    @Mock private StripeCheckoutService stripeCheckout;

    private JoinService joinService;

    @BeforeEach
    void setUp() {
        joinService = new JoinService(salons, plans, memberships, invoices, invites, inviteService, stripeCheckout);
    }

    // ---------- firstAmount ----------

    @Test
    void firstAmountIsZeroDuringTrial() {
        Plan plan = new Plan();
        plan.setTrialDays(7);
        plan.setPriceJpy(2980);
        plan.setIntroDiscount(0);
        assertEquals(0, joinService.firstAmount(plan));
    }

    @Test
    void firstAmountIsFlooredAtZeroWhenDiscountExceedsPrice() {
        Plan plan = new Plan();
        plan.setTrialDays(0);
        plan.setPriceJpy(500);
        plan.setIntroDiscount(1000);
        assertEquals(0, joinService.firstAmount(plan));
    }

    @Test
    void firstAmountIsPriceMinusDiscountOtherwise() {
        Plan plan = new Plan();
        plan.setTrialDays(0);
        plan.setPriceJpy(2980);
        plan.setIntroDiscount(500);
        assertEquals(2480, joinService.firstAmount(plan));
    }

    // ---------- inviteAllowed ----------

    private Salon salonWithVisibility(String visibility) {
        Salon salon = new Salon();
        salon.setVisibility(visibility);
        return salon;
    }

    @Test
    void publicSalonIsAlwaysAllowed() {
        Salon salon = salonWithVisibility("public");
        assertTrue(joinService.inviteAllowed(salon, false, false, null));
    }

    @Test
    void inviteSalonAllowsActiveMemberWithoutCode() {
        Salon salon = salonWithVisibility("invite");
        assertTrue(joinService.inviteAllowed(salon, true, false, null));
    }

    @Test
    void inviteSalonAllowsOwnerWithoutCode() {
        Salon salon = salonWithVisibility("invite");
        assertTrue(joinService.inviteAllowed(salon, false, true, null));
    }

    @Test
    void inviteSalonAllowsNonMemberWithValidCode() {
        Salon salon = salonWithVisibility("invite");
        salon.setId("salon-1");
        when(inviteService.isValid("salon-1", "good-code")).thenReturn(true);
        assertTrue(joinService.inviteAllowed(salon, false, false, "good-code"));
    }

    @Test
    void inviteSalonBlocksNonMemberWithInvalidCode() {
        Salon salon = salonWithVisibility("invite");
        salon.setId("salon-1");
        when(inviteService.isValid("salon-1", "bad-code")).thenReturn(false);
        assertFalse(joinService.inviteAllowed(salon, false, false, "bad-code"));
    }

    // ---------- join（Stripe Checkout 分岐） ----------

    @Test
    void joinRedirectsToStripeCheckoutWhenEnabledAndDoesNotSaveMembership() {
        Salon salon = salonWithVisibility("public");
        salon.setId("salon-1");
        when(salons.findById("salon-1")).thenReturn(Optional.of(salon));
        when(memberships.findByUserIdAndSalonId("user-1", "salon-1")).thenReturn(Optional.empty());

        Plan plan = new Plan();
        plan.setId("plan-1");
        plan.setSalonId("salon-1");
        when(plans.findById("plan-1")).thenReturn(Optional.of(plan));

        when(stripeCheckout.enabled()).thenReturn(true);
        when(stripeCheckout.createCheckoutUrl(any(), eq("salon-1"), eq(plan), any(), any()))
                .thenReturn("https://checkout.stripe.com/c/pay/xxx");

        User me = new User();
        me.setId("user-1");

        String result = joinService.join(me, "salon-1", "plan-1", null, "https://example.com");

        assertEquals("https://checkout.stripe.com/c/pay/xxx", result);
        verify(memberships, never()).save(any());
    }

    // ---------- confirmStripeJoin ----------

    @Test
    void confirmStripeJoinCreatesActiveMembershipAndInvoiceForNewJoin() {
        when(memberships.findByUserIdAndSalonId("user-1", "salon-1")).thenReturn(Optional.empty());
        Plan plan = new Plan();
        plan.setId("plan-1");
        plan.setTrialDays(0);
        plan.setPriceJpy(2980);
        plan.setIntroDiscount(0);
        when(plans.findById("plan-1")).thenReturn(Optional.of(plan));

        String path = joinService.confirmStripeJoin("user-1", "salon-1", "plan-1", "sub_123");

        assertEquals("/salons/salon-1/home?msg=joined", path);

        ArgumentCaptor<Membership> captor = ArgumentCaptor.forClass(Membership.class);
        verify(memberships).save(captor.capture());
        Membership saved = captor.getValue();
        assertEquals("user-1", saved.getUserId());
        assertEquals("salon-1", saved.getSalonId());
        assertEquals("plan-1", saved.getPlanId());
        assertEquals("sub_123", saved.getStripeSubscriptionId());
        assertEquals(MembershipStatus.ACTIVE, saved.getStatus());

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoices).save(invoiceCaptor.capture());
        assertEquals(2980, invoiceCaptor.getValue().getAmountJpy());
    }

    @Test
    void confirmStripeJoinReactivatesExistingCancelledMembership() {
        Membership existing = new Membership();
        existing.setUserId("user-1");
        existing.setSalonId("salon-1");
        existing.setPlanId("old-plan");
        existing.setStatus(MembershipStatus.CANCELLED);
        when(memberships.findByUserIdAndSalonId("user-1", "salon-1")).thenReturn(Optional.of(existing));

        Plan plan = new Plan();
        plan.setId("plan-1");
        plan.setTrialDays(7);
        plan.setPriceJpy(2980);
        when(plans.findById("plan-1")).thenReturn(Optional.of(plan));

        joinService.confirmStripeJoin("user-1", "salon-1", "plan-1", "sub_456");

        ArgumentCaptor<Membership> captor = ArgumentCaptor.forClass(Membership.class);
        verify(memberships).save(captor.capture());
        assertSame(existing, captor.getValue());
        assertEquals("plan-1", existing.getPlanId());
        assertEquals(MembershipStatus.ACTIVE, existing.getStatus());
        assertEquals("sub_456", existing.getStripeSubscriptionId());
    }

    @Test
    void confirmStripeJoinSavesMembershipEvenWhenPlanMissingButSkipsInvoice() {
        when(memberships.findByUserIdAndSalonId("user-1", "salon-1")).thenReturn(Optional.empty());
        when(plans.findById("missing-plan")).thenReturn(Optional.empty());

        String path = joinService.confirmStripeJoin("user-1", "salon-1", "missing-plan", "sub_789");

        assertEquals("/salons/salon-1/home?msg=joined", path);
        verify(memberships).save(any());
        verify(invoices, never()).save(any());
    }
}
