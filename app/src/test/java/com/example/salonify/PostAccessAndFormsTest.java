package com.example.salonify;

import com.example.salonify.support.Forms;
import com.example.salonify.support.PostAccess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PostAccessAndFormsTest {

    private final PostAccess postAccess = new PostAccess();

    @Test
    void ownerCanViewEverything() {
        assertTrue(postAccess.canView("plan:abc", null, true));
    }

    @Test
    void allVisibilityIsPublicToMembers() {
        assertTrue(postAccess.canView("all", null, false));
        assertTrue(postAccess.canView((String) null, null, false));
    }

    @Test
    void planLimitedRequiresMatchingPlan() {
        assertTrue(postAccess.canView("plan:abc", "abc", false));
        assertFalse(postAccess.canView("plan:abc", "xyz", false));
        assertFalse(postAccess.canView("plan:abc", null, false));
    }

    @Test
    void requiredPlanIdParsing() {
        assertEquals("abc", postAccess.requiredPlanId("plan:abc"));
        assertNull(postAccess.requiredPlanId("all"));
        assertNull(postAccess.requiredPlanId(null));
    }

    @Test
    void unknownVisibilityIsDeniedByDefault() {
        assertFalse(postAccess.canView("weird", null, false));
        assertFalse(postAccess.canView("plan;xxx", null, false));
        assertTrue(postAccess.canView((String) null, null, false));
        assertTrue(postAccess.canView("all", null, false));
    }

    @Test
    void ownerCanViewUnknownVisibilityToo() {
        assertTrue(postAccess.canView("weird", null, true));
    }

    @Test
    void formsSliceTruncates() {
        assertEquals("abc", Forms.slice("abcdef", 3));
        assertEquals("abc", Forms.slice("abc", 10));
        assertEquals("", Forms.slice(null, 5));
    }

    @Test
    void formsClampAndParse() {
        assertEquals(100000, Forms.clamp(999999, 0, 100000));
        assertEquals(0, Forms.clamp(-5, 0, 100000));
        assertEquals(30, Forms.clamp(99, 0, 30));
        assertEquals(7, Forms.parseInt("7", 0));
        assertEquals(0, Forms.parseInt("notanumber", 0));
        assertEquals(5, Forms.parseInt("", 5));
    }

    @Test
    void formsNullIfBlank() {
        assertNull(Forms.nullIfBlank(""));
        assertNull(Forms.nullIfBlank(null));
        assertEquals("x", Forms.nullIfBlank("x"));
    }
}
