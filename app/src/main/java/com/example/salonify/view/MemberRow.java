package com.example.salonify.view;

import com.example.salonify.entity.Invoice;
import com.example.salonify.entity.Membership;
import com.example.salonify.entity.Plan;
import com.example.salonify.entity.User;

/** owner/members.html のメンバー行用の view model。 */
public record MemberRow(Membership membership, User user, Plan plan, Invoice lastInvoice) {
}
