package com.example.salonify.service;

import com.example.salonify.entity.Invoice;
import com.example.salonify.entity.InvoiceStatus;
import com.example.salonify.entity.Membership;
import com.example.salonify.entity.MembershipStatus;
import com.example.salonify.entity.Plan;
import com.example.salonify.repository.InvoiceRepository;
import com.example.salonify.repository.MembershipRepository;
import com.example.salonify.repository.PlanRepository;
import com.example.salonify.support.Payment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RevenueService {

    private final MembershipRepository memberships;
    private final InvoiceRepository invoices;
    private final PlanRepository plans;
    private final Payment payment;

    public RevenueService(MembershipRepository memberships, InvoiceRepository invoices, PlanRepository plans,
                          Payment payment) {
        this.memberships = memberships;
        this.invoices = invoices;
        this.plans = plans;
        this.payment = payment;
    }

    public record InvoiceRow(Invoice invoice, int fee, int net) {}

    public record RevenueSummary(long total, long fee, long net, List<InvoiceRow> rows) {}

    public record MonthlyEstimate(List<Plan> plans, Map<String, Long> perPlanCount, long monthlyRevenue,
                                  int activeCount) {}

    /** サロンの支払い済みインボイスに基づく売上サマリー。fee/netの合計は各行のfee/netを合算したもの。 */
    public RevenueSummary revenueFor(String salonId) {
        List<String> membershipIds = memberships.findBySalonIdOrderByJoinedAtDesc(salonId)
                .stream().map(Membership::getId).toList();
        List<Invoice> paid = membershipIds.isEmpty() ? List.of()
                : invoices.findByMembershipIdInAndStatusOrderByCreatedAtDesc(membershipIds, InvoiceStatus.PAID);

        long total = 0;
        long fee = 0;
        long net = 0;
        List<InvoiceRow> rows = new ArrayList<>();
        for (Invoice inv : paid) {
            total += inv.getAmountJpy();
            int rowFee = payment.platformFee(inv.getAmountJpy());
            int rowNet = payment.ownerNet(inv.getAmountJpy());
            fee += rowFee;
            net += rowNet;
            rows.add(new InvoiceRow(inv, rowFee, rowNet));
        }

        return new RevenueSummary(total, fee, net, rows);
    }

    /** プラットフォーム全体の支払い済みインボイスの合計額。 */
    public long totalPaidRevenue() {
        return invoices.findByStatus(InvoiceStatus.PAID).stream().mapToLong(Invoice::getAmountJpy).sum();
    }

    /** 現在のアクティブなメンバーシップに基づくサロンの月間売上予測。 */
    public MonthlyEstimate monthlyEstimate(String salonId) {
        List<Plan> planList = plans.findBySalonIdOrderByPriceJpyAsc(salonId);
        List<Membership> active = memberships.findBySalonIdAndStatus(salonId, MembershipStatus.ACTIVE);

        long monthlyRevenue = 0;
        Map<String, Long> perPlanCount = new LinkedHashMap<>();
        for (Plan p : planList) {
            long c = active.stream().filter(m -> m.getPlanId().equals(p.getId())).count();
            perPlanCount.put(p.getId(), c);
            monthlyRevenue += (long) p.getPriceJpy() * c;
        }

        return new MonthlyEstimate(planList, perPlanCount, monthlyRevenue, active.size());
    }
}
