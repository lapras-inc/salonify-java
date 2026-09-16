package com.example.salonify.view;

import com.example.salonify.entity.Salon;

/** admin/salons.html のサロン行用の view model。 */
public record AdminSalonRow(Salon salon, String ownerEmail, long memberCount) {
}
