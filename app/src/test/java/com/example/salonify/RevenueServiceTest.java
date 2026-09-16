package com.example.salonify;

import com.example.salonify.entity.Invoice;
import com.example.salonify.entity.Membership;
import com.example.salonify.repository.InvoiceRepository;
import com.example.salonify.repository.MembershipRepository;
import com.example.salonify.repository.PlanRepository;
import com.example.salonify.service.RevenueService;
import com.example.salonify.support.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueServiceTest {

    @Mock private MembershipRepository memberships;
    @Mock private InvoiceRepository invoices;
    @Mock private PlanRepository plans;

    private RevenueService revenueService;

    @BeforeEach
    void setUp() {
        revenueService = new RevenueService(memberships, invoices, plans, new Payment());
    }

    private Invoice invoice(int amountJpy) {
        Invoice inv = new Invoice();
        inv.setAmountJpy(amountJpy);
        inv.setStatus("paid");
        return inv;
    }

    @Test
    void totalFeeIsSumOfRowFeesNotFeeOfTotal() {
        String salonId = "salon-1";
        List<Membership> ms = List.of(new Membership(), new Membership());
        List<String> membershipIds = ms.stream().map(Membership::getId).toList();
        when(memberships.findBySalonIdOrderByJoinedAtDesc(salonId)).thenReturn(ms);
        List<Invoice> paid = List.of(invoice(999), invoice(999));
        when(invoices.findByMembershipIdInAndStatusOrderByCreatedAtDesc(membershipIds, "paid"))
                .thenReturn(paid);

        RevenueService.RevenueSummary summary = revenueService.revenueFor(salonId);

        // floor(999 * 0.05) = 49（1行あたり） -> 49 + 49 = 98。floor(1998 * 0.05) = 99 ではない。
        assertEquals(1998, summary.total());
        assertEquals(98, summary.fee());
        assertEquals(1998 - 98, summary.net());
        assertEquals(2, summary.rows().size());
        assertEquals(49, summary.rows().get(0).fee());
        assertEquals(49, summary.rows().get(1).fee());
    }

    @Test
    void emptySalonHasZeroSummary() {
        String salonId = "salon-empty";
        when(memberships.findBySalonIdOrderByJoinedAtDesc(salonId)).thenReturn(List.of());

        RevenueService.RevenueSummary summary = revenueService.revenueFor(salonId);

        assertEquals(0, summary.total());
        assertEquals(0, summary.fee());
        assertEquals(0, summary.net());
        assertEquals(0, summary.rows().size());
    }
}
