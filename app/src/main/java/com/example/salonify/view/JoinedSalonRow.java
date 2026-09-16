package com.example.salonify.view;

import com.example.salonify.entity.Plan;
import com.example.salonify.entity.Salon;

/** dashboard.html の参加済みサロン行用の view model。 */
public record JoinedSalonRow(Salon salon, Plan plan) {
}
