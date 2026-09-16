package com.example.salonify;

import com.example.salonify.entity.SalonVisibility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SalonVisibilityTest {

    @Test
    void publicPassesThrough() {
        assertEquals(SalonVisibility.PUBLIC, SalonVisibility.normalize("public"));
    }

    @Test
    void invitePassesThrough() {
        assertEquals(SalonVisibility.INVITE, SalonVisibility.normalize("invite"));
    }

    @Test
    void unknownOrNullFallsBackToPublic() {
        assertEquals(SalonVisibility.PUBLIC, SalonVisibility.normalize("private"));
        assertEquals(SalonVisibility.PUBLIC, SalonVisibility.normalize("weird"));
        assertEquals(SalonVisibility.PUBLIC, SalonVisibility.normalize(null));
        assertEquals(SalonVisibility.PUBLIC, SalonVisibility.normalize(""));
    }
}
