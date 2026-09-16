package com.example.salonify;

import com.example.salonify.entity.Plan;
import com.example.salonify.repository.PlanRepository;
import com.example.salonify.service.PostViewService;
import com.example.salonify.support.PostAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostViewServiceTest {

    @Mock private PlanRepository plans;

    private PostViewService postViewService;

    @BeforeEach
    void setUp() {
        postViewService = new PostViewService(plans, new PostAccess());
    }

    private Plan planFor(String salonId) {
        Plan plan = new Plan();
        plan.setSalonId(salonId);
        return plan;
    }

    @Test
    void nullVisibilityNormalizesToAll() {
        assertEquals(PostAccess.ALL, postViewService.normalizeVisibility("salon-1", null));
    }

    @Test
    void blankVisibilityNormalizesToAll() {
        assertEquals(PostAccess.ALL, postViewService.normalizeVisibility("salon-1", ""));
    }

    @Test
    void allVisibilityPassesThrough() {
        assertEquals(PostAccess.ALL, postViewService.normalizeVisibility("salon-1", PostAccess.ALL));
    }

    @Test
    void matchingSalonPlanPassesThrough() {
        when(plans.findById("plan-1")).thenReturn(Optional.of(planFor("salon-1")));
        String visibility = PostAccess.planVisibility("plan-1");
        assertEquals(visibility, postViewService.normalizeVisibility("salon-1", visibility));
    }

    @Test
    void planFromAnotherSalonFallsBackToAll() {
        when(plans.findById("plan-1")).thenReturn(Optional.of(planFor("other-salon")));
        String visibility = PostAccess.planVisibility("plan-1");
        assertEquals(PostAccess.ALL, postViewService.normalizeVisibility("salon-1", visibility));
    }

    @Test
    void nonexistentPlanFallsBackToAll() {
        when(plans.findById("missing")).thenReturn(Optional.empty());
        String visibility = PostAccess.planVisibility("missing");
        assertEquals(PostAccess.ALL, postViewService.normalizeVisibility("salon-1", visibility));
    }
}
