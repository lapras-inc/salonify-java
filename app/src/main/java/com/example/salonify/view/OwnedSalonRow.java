package com.example.salonify.view;

import com.example.salonify.entity.Salon;

/** dashboard.html のオーナー保有サロン行用の view model。 */
public record OwnedSalonRow(Salon salon, long memberCount) {
}
