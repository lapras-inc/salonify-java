package com.example.salonify.view;

import com.example.salonify.entity.Invoice;
import com.example.salonify.entity.Membership;
import com.example.salonify.entity.Plan;
import com.example.salonify.entity.Salon;

import java.util.List;

/** account/billing.html の請求行用の view model。 */
public record BillingRow(Membership membership, Salon salon, Plan plan, List<Invoice> invoices) {
}
